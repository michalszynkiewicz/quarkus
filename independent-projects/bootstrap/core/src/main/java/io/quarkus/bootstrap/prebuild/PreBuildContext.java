package io.quarkus.bootstrap.prebuild;

import io.quarkus.bootstrap.resolver.AppModelResolver;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Context for the pre-compilation (generate sources) phase.
 * Usually used by methods annotated with @PreBuildStep
 */
public class PreBuildContext {
    private static volatile PreBuildContext instance;

    public final AppModelResolver appModelResolver;

    public final Path sourcesDir;
    public final Path buildDir;
    public final boolean test;

    public final Consumer<Path> compileSourceRegistrar;

    private PreBuildContext(AppModelResolver appModelResolver,
            Path sourcesDir, Path buildDir,
            Consumer<Path> compileSourceRegistrar,
            boolean test) {
        this.appModelResolver = appModelResolver;
        this.sourcesDir = sourcesDir;
        this.buildDir = buildDir;
        this.compileSourceRegistrar = compileSourceRegistrar;
        this.test = test;
    }

    public static PreBuildContext get() {
        if (instance == null) {
            throw new PreBuildException("PreBuildContext not initialized!");
        }
        return instance;
    }

    public static void initialize(AppModelResolver appModelResolver,
            Path buildDir,
            Path sourcesDir,
            Consumer<Path> compileSourceRegistrar,
            boolean isTest) {
        instance = new PreBuildContext(appModelResolver, sourcesDir, buildDir, compileSourceRegistrar, isTest);
    }

    public boolean isTest() {
        return test;
    }
}
