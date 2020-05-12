package io.quarkus.grpc.deployment;

import static java.util.Arrays.asList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.PreBuildContext;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.deployment.annotations.PreBuildStep;
import io.quarkus.utilities.JavaBinFinder;
import io.quarkus.utilities.OS;

// mstodo test windows
// mstodo customize via properties
// mstodo customizable proto path
public class PreBuildProcessor {
    private static final String CLASS_PATH_DELIMITER = "" + File.pathSeparatorChar;

    private static final String quarkusProtocPluginMain = "io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator";

    private Executables executables;

    // msTODO: the real thing adds some google stuff
    @PreBuildStep
    public void fire() throws IOException, InterruptedException, AppModelResolverException {
        Path protoDir = Paths.get("src", "main", "proto");
        Path protoTestDir = Paths.get("src", "test", "proto");
        Path buildDir = PreBuildContext.get().buildDir;
        Path outDir = buildDir.resolve("generated-sources").resolve("protobuf");
        Path testOutDir = buildDir.resolve("generated-test-sources").resolve("protobuf");

        generateProtoClasses(protoDir, PreBuildContext.get().compileSourceRegistrar, buildDir, outDir);

        generateProtoClasses(protoTestDir, PreBuildContext.get().testSourceRegistrar, buildDir, testOutDir);
    }

    private void generateProtoClasses(Path protoDir,
            Consumer<Path> sourceRegistrar,
            Path buildDir,
            Path outDir) throws IOException, AppModelResolverException, InterruptedException {
        if (Files.isDirectory(protoDir)) {
            List<String> protoFiles = Files.walk(protoDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".proto"))
                    .map(Path::toString)
                    .collect(Collectors.toList());
            System.out.println("Found proto files: " + protoFiles);// mstodo replace with logging
            if (!protoFiles.isEmpty()) {
                mkdir(buildDir);

                Path grpcOut = mkOutDir(sourceRegistrar, outDir, "grpc");
                Path quarkusGrpcOut = mkOutDir(sourceRegistrar, outDir, "quarkus-grpc");
                Path protoOut = mkOutDir(sourceRegistrar, outDir, "proto");

                AppModelResolver resolver = PreBuildContext.get().appModelResolver;

                executables = executables == null ? getExecutables(buildDir, resolver) : executables;

                List<String> command = new ArrayList<>();
                command.addAll(asList(executables.protoc.toString(),
                        "-I=" + protoDir.toString(),
                        "--plugin=protoc-gen-grpc=" + executables.grpc,
                        "--plugin=protoc-gen-q-grpc=" + executables.quarkusGrpc,
                        "--q-grpc_out=" + quarkusGrpcOut,
                        "--grpc_out=" + grpcOut,
                        "--java_out=" + protoOut));
                command.addAll(protoFiles);

                Process process = new ProcessBuilder()
                        .command(command)
                        .inheritIO().start();
                int resultCode = process.waitFor();
                if (resultCode != 0) {
                    throw new RuntimeException("Failed to generate Java classes from proto file: " + protoFiles);
                    // mstodo look at all the RuntimeExceptions here and ? change to specific exceptions
                }
            }
        }
    }

    private Path mkOutDir(Consumer<Path> sourceRegistrar, Path outDir, String proto) throws IOException {
        Path protoOut = outDir.resolve(proto);
        sourceRegistrar.accept(protoOut);
        mkdir(protoOut);
        return protoOut;
    }

    private Executables getExecutables(Path buildDir, AppModelResolver resolver) throws IOException, AppModelResolverException {
        // mstodo customizability?
        // mstodo protoc version, protoc-gen-grpc-java version, maybe groupId:artifactId?
        Path protocExe = prepareExecutable(buildDir, resolver,
                new AppArtifact("com.google.protobuf", "protoc", osClassifier(), "exe", "3.5.1"), "protoc.exe");
        Path protocGrpcPluginExe = prepareExecutable(buildDir, resolver,
                new AppArtifact("io.grpc", "protoc-gen-grpc-java", osClassifier(), "exe", "1.25.0"), "protocGrpc.exe");

        Path quarkusGrpcPluginExe = prepareQuarkusGrpcExecutable(resolver, buildDir);

        return new Executables(protocExe, protocGrpcPluginExe, quarkusGrpcPluginExe);
    }

    private Path prepareExecutable(Path buildDir, AppModelResolver resolver, AppArtifact a, String exeName) throws IOException {
        Path exeArtifact;
        try {
            exeArtifact = resolver.resolve(a);
        } catch (AppModelResolverException e) {
            throw new RuntimeException("Unable to resolve the executable: " + a +
                    ". It may mean you are using an unsupported architecture. Consider switching" +
                    " to maven-protoc-plugin for java classes generation", e);
        }
        Path exe = buildDir.resolve(exeName);
        Files.copy(exeArtifact, exe);
        if (!exe.toFile().setExecutable(true)) {
            throw new RuntimeException("Failed to make the file executable: " + exe);
        }
        return exe;
    }

    private void mkdir(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    private String osClassifier() {
        String architecture = OS.getArchitecture();
        switch (OS.determineOS()) {
            case LINUX:
                return "linux-" + architecture;
            case WINDOWS:
                return "windows-" + architecture;
            case MAC:
                return "osx-" + architecture;
            default:
                throw new RuntimeException(
                        "Unsupported OS, please use maven plugin instead to generate Java classes from proto files"); // mstodo 
        }
    }

    private static Path prepareQuarkusGrpcExecutable(AppModelResolver resolver,
            Path buildDir)
            throws IOException, AppModelResolverException {
        // mstodo get this version from somewhere
        String quarkusGrpcVersion = "999-SNAPSHOT";
        AppArtifact quarkusGrpcPlugin = new AppArtifact("io.quarkus", "quarkus-grpc-protoc-plugin", quarkusGrpcVersion);
        AppModel qGrpcPluginModel = resolver.resolveModel(quarkusGrpcPlugin);
        String classpath = qGrpcPluginModel.getUserDependencies().stream()
                .map(AppDependency::getArtifact)
                .map(a -> getArtifact(resolver, a))
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .collect(Collectors.joining(CLASS_PATH_DELIMITER));
        if (!classpath.isEmpty()) {
            classpath += CLASS_PATH_DELIMITER;
        }
        classpath += getArtifact(resolver, quarkusGrpcPlugin).toAbsolutePath().toString();

        Path script;
        BufferedWriter writer;
        if (OS.determineOS() != OS.WINDOWS) {
            script = Files.createTempFile(buildDir, "quarkus-grpc", ".sh");

            writer = Files.newBufferedWriter(script);

            writer.write("#!/bin/sh\n");
            writer.newLine();
        } else {
            script = Files.createTempFile(buildDir, "quarkus-grpc", ".cmd");
            writer = Files.newBufferedWriter(script);
        }
        writer.write(JavaBinFinder.findBin() + " -cp " +
                classpath + " " + quarkusProtocPluginMain);
        writer.newLine();
        writer.close();

        if (!script.toFile().setExecutable(true)) {
            System.out.println("failed to set file: " + script + " executable. Protoc invocation may fail");
            // mstodo log instead
        }
        return script;
    }

    private static Path getArtifact(AppModelResolver resolver, AppArtifact a) {
        try {
            return resolver.resolve(a);
        } catch (AppModelResolverException e) {
            throw new RuntimeException("Failed to resolve artifact " + a, e);
        }
    }

    private static class Executables {
        private final Path protoc;
        private final Path grpc;
        private final Path quarkusGrpc;

        private Executables(Path protoc, Path grpc, Path quarkusGrpc) {
            this.protoc = protoc;
            this.grpc = grpc;
            this.quarkusGrpc = quarkusGrpc;
        }
    }
}
