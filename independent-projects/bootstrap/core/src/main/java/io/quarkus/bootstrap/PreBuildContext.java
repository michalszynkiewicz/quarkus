package io.quarkus.bootstrap;

import io.quarkus.bootstrap.resolver.AppModelResolver;
import java.nio.file.Path;
import java.util.function.Consumer;

public class PreBuildContext {
    // mstodo change to build item?
    // mstodo placement?
    private static volatile PreBuildContext instance;

    public final AppModelResolver appModelResolver;

    public final Path sourcesDir;
    public final Path testSourcesDir;
    public final Path buildDir;

    public final Consumer<Path> compileSourceRegistrar;
    public final Consumer<Path> testSourceRegistrar;

    private PreBuildContext(AppModelResolver appModelResolver,
            Path sourcesDir, Path testSourcesDir, Path buildDir,
            Consumer<Path> compileSourceRegistrar,
            Consumer<Path> testSourceRegistrar) {
        this.appModelResolver = appModelResolver;
        this.sourcesDir = sourcesDir;
        this.testSourcesDir = testSourcesDir;
        this.buildDir = buildDir;
        this.compileSourceRegistrar = compileSourceRegistrar;
        this.testSourceRegistrar = testSourceRegistrar;
    }

    public static PreBuildContext get() {
        return instance;
    }

    public static void initialize(AppModelResolver appModelResolver,
            Path buildDir,
            Path sourcesDir,
            Path testSourcesDir,
            Consumer<Path> compileSourceRegistrar,
            Consumer<Path> testSourceRegistrar) {
        instance = new PreBuildContext(appModelResolver, sourcesDir, testSourcesDir, buildDir, compileSourceRegistrar,
                testSourceRegistrar);
    }
}
