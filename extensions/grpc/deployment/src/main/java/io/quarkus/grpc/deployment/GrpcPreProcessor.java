package io.quarkus.grpc.deployment;

import static java.util.Arrays.asList;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.prebuild.PreBuildContext;
import io.quarkus.bootstrap.prebuild.PreBuildException;
import io.quarkus.bootstrap.prebuild.PreBuildFailureException;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.deployment.annotations.PreBuildStep;
import io.quarkus.utilities.JavaBinFinder;
import io.quarkus.utilities.OS;

public class GrpcPreProcessor {
    private static final String quarkusProtocPluginMain = "io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator";

    private Executables executables;

    // msTODO: the original thing adds some google stuff ???
    @PreBuildStep
    public void fire() {
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
            Path outDir) {
        try {
            if (Files.isDirectory(protoDir)) {
                List<String> protoFiles = Files.walk(protoDir)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".proto"))
                        .map(Path::toString)
                        .collect(Collectors.toList());
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
                            .inheritIO()
                            .start();
                    int resultCode = process.waitFor();
                    if (resultCode != 0) {
                        throw new PreBuildException("Failed to generate Java classes from proto file: " + protoFiles);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new PreBuildException("Failed to generate java files from proto file in " + protoDir.toAbsolutePath(), e);
        }
    }

    private Path mkOutDir(Consumer<Path> sourceRegistrar, Path outDir, String proto) throws IOException {
        Path protoOut = outDir.resolve(proto);
        sourceRegistrar.accept(protoOut);
        mkdir(protoOut);
        return protoOut;
    }

    private Executables getExecutables(Path buildDir, AppModelResolver resolver) throws IOException {
        Path protocExe = prepareExecutable(buildDir, resolver,
                new AppArtifact("com.google.protobuf", "protoc", osClassifier(), "exe", "3.5.1"), "protoc.exe");
        Path protocGrpcPluginExe = prepareExecutable(buildDir, resolver,
                new AppArtifact("io.grpc", "protoc-gen-grpc-java", osClassifier(), "exe", "1.25.0"), "protocGrpc.exe");

        Path quarkusGrpcPluginExe = prepareQuarkusGrpcExecutable(resolver, buildDir);

        return new Executables(protocExe, protocGrpcPluginExe, quarkusGrpcPluginExe);
    }

    private Path prepareExecutable(Path buildDir, AppModelResolver resolver, AppArtifact a, String exeName) throws IOException {
        Path exeArtifact;
        Path exe = Files.createTempFile(buildDir, exeName, ".exe");
        // only download the artifact if we don't have it yet:
        try {
            exeArtifact = resolver.resolve(a);
        } catch (AppModelResolverException e) {
            throw new PreBuildException("Unable to resolve the executable: " + a +
                    ". It may mean you are using an unsupported architecture. Consider switching" +
                    " to maven-protoc-plugin for java classes generation", e);
        }
        Files.copy(exeArtifact, exe, StandardCopyOption.REPLACE_EXISTING);
        if (!exe.toFile().setExecutable(true)) {
            throw new PreBuildException("Failed to make the file executable: " + exe);
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
                throw new PreBuildException(
                        "Unsupported OS, please use maven plugin instead to generate Java classes from proto files");
        }
    }

    private static Path prepareQuarkusGrpcExecutable(AppModelResolver resolver, Path buildDir) throws IOException {
        String quarkusGrpcVersion = GrpcPreProcessor.class.getPackage().getImplementationVersion();
        AppArtifact quarkusGrpcPlugin = new AppArtifact("io.quarkus", "quarkus-grpc-protoc-plugin", quarkusGrpcVersion);
        Path pluginPath;
        try {
            pluginPath = resolver.resolve(quarkusGrpcPlugin);
        } catch (AppModelResolverException e) {
            throw new PreBuildException("Failed to resolve quarkus grpc protoc plugin", e);
        }

        if (OS.determineOS() != OS.WINDOWS) {
            return writeScript(buildDir, pluginPath, "#!/bin/sh\n", ".sh");
        } else {
            return writeScript(buildDir, pluginPath, "", ".cmd");
        }
    }

    private static Path writeScript(Path buildDir, Path pluginPath, String shebang, String suffix) throws IOException {
        Path script = Files.createTempFile(buildDir, "quarkus-grpc", suffix);
        try (BufferedWriter writer = Files.newBufferedWriter(script)) {
            writer.write(shebang);
            writePluginExeCmd(pluginPath, writer);
        }
        if (!script.toFile().setExecutable(true)) {
            throw new PreBuildFailureException("failed to set file: " + script + " executable. Protoc invocation may fail");
        }
        return script;
    }

    private static void writePluginExeCmd(Path pluginPath, BufferedWriter writer) throws IOException {
        writer.write(JavaBinFinder.findBin() + " -cp " +
                pluginPath.toAbsolutePath().toString() + " " + quarkusProtocPluginMain);
        writer.newLine();
    }

    private static class Executables {
        final Path protoc;
        final Path grpc;
        final Path quarkusGrpc;

        Executables(Path protoc, Path grpc, Path quarkusGrpc) {
            this.protoc = protoc;
            this.grpc = grpc;
            this.quarkusGrpc = quarkusGrpc;
        }
    }
}
