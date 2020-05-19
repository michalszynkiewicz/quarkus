package io.quarkus.grpc.deployment;

import static java.util.Arrays.asList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.PreBuildContext;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.deployment.annotations.PreBuildStep;
import io.quarkus.utilities.JavaBinFinder;
import io.quarkus.utilities.OS;

// mstodo test windows
// mstodo customize via properties
// mstodo customizable proto path
public class PreBuildProcessor { // mstodo GrpcPreProcessor
    private static final String CLASS_PATH_DELIMITER = "" + File.pathSeparatorChar;

    private static final String quarkusProtocPluginMain = "io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator";

    private Executables executables;

    // msTODO: the real thing adds some google stuff
    @PreBuildStep
    public void fire() throws IOException, InterruptedException, AppModelResolverException {
        Path protoDir = PreBuildContext.get().sourcesDir.resolve("proto");
        Path protoTestDir = PreBuildContext.get().testSourcesDir.resolve("proto");
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

                // mstodo rollback output redirection?
                ProcessBuilder builder = new ProcessBuilder()
                        .command(command);
                builder
                        .redirectErrorStream(true)
                        .redirectOutput(File.createTempFile("from-graddily-doo", ".txt"));
                Process process = builder.start();
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
        Path exe = buildDir.resolve(exeName);
        if (!Files.exists(exe)) {
            // only download the artifact if we don't have it yet:
            try {
                exeArtifact = resolver.resolve(a);
            } catch (AppModelResolverException e) {
                throw new RuntimeException("Unable to resolve the executable: " + a +
                        ". It may mean you are using an unsupported architecture. Consider switching" +
                        " to maven-protoc-plugin for java classes generation", e);
            }
            Files.copy(exeArtifact, exe);
            if (!exe.toFile().setExecutable(true)) {
                throw new RuntimeException("Failed to make the file executable: " + exe);
            }
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
        List<Path> qGrpcPluginClasspath = new ArrayList<>();

        String quarkusGrpcVersion = PreBuildProcessor.class.getPackage().getImplementationVersion();
        AppArtifact quarkusGrpcPlugin = new AppArtifact("io.quarkus", "quarkus-grpc-protoc-plugin", quarkusGrpcVersion);
        Path pluginPath = resolver.resolve(quarkusGrpcPlugin);
        qGrpcPluginClasspath.add(pluginPath);
        List<AppArtifact> classpathArtifacts = readClasspath(pluginPath);
        for (AppArtifact classpathArtifact : classpathArtifacts) {
            Path dependencypath = resolver.resolve(classpathArtifact);
            qGrpcPluginClasspath.add(dependencypath);
        }

        String classpath = qGrpcPluginClasspath.stream()
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .collect(Collectors.joining(CLASS_PATH_DELIMITER));
        if (!classpath.isEmpty()) {
            classpath += CLASS_PATH_DELIMITER;
        }
        classpath += getArtifact(resolver, quarkusGrpcPlugin).toAbsolutePath().toString();

        Path script;
        BufferedWriter writer = null;
        try {
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
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        if (!script.toFile().setExecutable(true)) {
            System.out.println("failed to set file: " + script + " executable. Protoc invocation may fail");
            // mstodo log instead
        }
        return script;
    }

    private static List<AppArtifact> readClasspath(Path pluginPath) {
        List<AppArtifact> result = new ArrayList<>();
        try (FileInputStream stream = new FileInputStream(pluginPath.toFile());
                JarInputStream jarStream = new JarInputStream(stream)) {
            Manifest manifest = jarStream.getManifest();
            String classPath = (String) manifest.getMainAttributes().get(new Attributes.Name("Class-Path"));
            String[] artifacts = classPath.split(Pattern.quote(" ")); // mstodo check multiple artifacts on the classpath
            for (String artifact : artifacts) {
                String[] gav_cp = artifact.split(":");
                if (gav_cp.length < 4 || gav_cp.length > 5) {
                    throw new RuntimeException(
                            "Invalid artifact specifier:  " + artifact + " in the classpath of the qarkus grpc plugin");
                }
                String groupId = gav_cp[0];
                String artifactId = gav_cp[1];
                String version = gav_cp[2];
                String classifier = gav_cp.length == 4 ? null : gav_cp[3];
                String packaging = gav_cp.length == 4 ? gav_cp[3] : gav_cp[4];

                result.add(new AppArtifact(groupId, artifactId, classifier, packaging, version));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failure constructing classpath"); // mstodo
        }
        return result;
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
