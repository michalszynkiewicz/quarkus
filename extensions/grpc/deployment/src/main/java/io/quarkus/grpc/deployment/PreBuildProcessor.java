package io.quarkus.grpc.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.PreBuildContext;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.deployment.annotations.PreBuildStep;

// mstodo test for multiple protoc files
public class PreBuildProcessor {
    private static final String CLASS_PATH_DELIMITER = "" + File.pathSeparatorChar;

    private static final String quarkusProtocPluginMain = "io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator";

    // msTODO: the real thing adds some google stuff
    @PreBuildStep
    public void fire() throws IOException, InterruptedException, AppModelResolverException {
        Path protoDir = Paths.get("src", "main", "proto");
        Path protoTestDir = Paths.get("src", "test", "proto");
        Executables executables = null;
        if (Files.isDirectory(protoDir)) {
            String protoFiles = Files.walk(protoDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".proto"))
                    .map(Path::toString)
                    .collect(Collectors.joining(" ")); // mstodo switch to doing an invo at a time if ain't working
            System.out.println("Found proto files: " + protoFiles);// mstodo replace with logging
            if (!protoFiles.isEmpty()) {
                Path buildDir = PreBuildContext.get().buildDir;
                mkdir(buildDir);
                Path protoBufDir = buildDir.resolve("generated-sources").resolve("protobuf");
                Path grpcOut = protoBufDir.resolve("grpc");
                Path quarkusGrpcOut = protoBufDir.resolve("quarkus-grpc");
                Path protoOut = protoBufDir.resolve("proto");

                PreBuildContext.get().compileSourceRegistrar.accept(grpcOut);
                PreBuildContext.get().compileSourceRegistrar.accept(quarkusGrpcOut);
                PreBuildContext.get().compileSourceRegistrar.accept(protoOut);

                mkdir(grpcOut);
                mkdir(quarkusGrpcOut);
                mkdir(protoOut);

                AppModelResolver resolver = PreBuildContext.get().appModelResolver;
                // String groupId, String artifactId, String classifier, String type, String version
                // mstodo customize via properties

                executables = getExecutables(buildDir, resolver);

                Process process = new ProcessBuilder()
                        .command(executables.protoc.toString(),
                                "-I=" + protoDir.toString(),
                                "--plugin=protoc-gen-grpc=" + executables.grpc,
                                "--plugin=protoc-gen-q-grpc=" + executables.quarkusGrpc,
                                // mstodo paths
                                "--q-grpc_out=" + quarkusGrpcOut,
                                "--grpc_out=" + grpcOut,
                                "--java_out=" + protoOut, // mstodo different target for gradle?
                                protoFiles) // mstodo: put the test ones in the test sources and the prod ones in the prod
                        .inheritIO().start();
                int resultCode = process.waitFor();
                if (resultCode != 0) {
                    throw new RuntimeException("Failed to generate Java classes from proto file: " + protoFiles); // mstodo: specific exception?
                }
            }
        }
    }

    private Executables getExecutables(Path buildDir, AppModelResolver resolver) throws IOException, AppModelResolverException {
        Path protoc = getArtifact(resolver,
                new AppArtifact("com.google.protobuf", "protoc", osClassifier(), "exe", "3.5.1"));
        Path grpcPlugin = getArtifact(resolver,
                new AppArtifact("io.grpc", "protoc-gen-grpc-java", osClassifier(), "exe", "1.25.0"));
        Path protocExe = buildDir.resolve("protoc.exe");
        Files.copy(protoc, protocExe);
        Path protocGrpcPluginExe = buildDir.resolve("protocGrpc.exe");
        Files.copy(grpcPlugin, protocGrpcPluginExe);

        Files.setPosixFilePermissions(protocExe, PosixFilePermissions.fromString("rwx------"));
        Files.setPosixFilePermissions(protocGrpcPluginExe, PosixFilePermissions.fromString("rwx------"));

        Path quarkusGrpcPluginExe = prepareQuarkusGrpcExecutable(resolver);

        return new Executables(protocExe, protocGrpcPluginExe, quarkusGrpcPluginExe);
    }

    private void mkdir(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    private String osClassifier() {
        return "linux-x86_64";
    }

    private static Path prepareQuarkusGrpcExecutable(AppModelResolver resolver)
            throws IOException, AppModelResolverException { // mstodo os as a parameter
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

        // mstodo append all the deps to classpath:
        File scriptFile = File.createTempFile("quarkus-grpc", ".sh");

        String script = "#!/bin/sh\n" + // mstodo windows support
                System.getProperty("java.home") + "/bin/java" + " -cp " +
                classpath + " " + quarkusProtocPluginMain + "\n";

        Files.write(scriptFile.toPath(), script.getBytes());
        Files.setPosixFilePermissions(scriptFile.toPath(), PosixFilePermissions.fromString("rwx------"));
        scriptFile.deleteOnExit();
        return scriptFile.toPath();
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
