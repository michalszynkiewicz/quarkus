package io.quarkus.deployment;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.function.Consumer;

import io.quarkus.bootstrap.prebuild.PreBuildContext;
import io.quarkus.bootstrap.prebuild.PreBuildException;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.deployment.annotations.PreBuildStep;
import io.quarkus.deployment.util.ServiceUtil;

public class PreBuilder {
    public static void prepareSources(ClassLoader deploymentClassLoader,
            Path buildDir,
            Path sourcesDir,
            Path testSourcesDir,
            AppModelResolver appModelResolver,
            Consumer<Path> compileSourceRegistrar,
            Consumer<Path> testSourceRegistrar) {
        Thread.currentThread().setContextClassLoader(deploymentClassLoader);
        Class<? extends Annotation> preBuildStepClass = getPreBuildStepClass(deploymentClassLoader);

        // gather pre-build steps
        Iterable<Class<?>> classes = getPreBuildClasses(deploymentClassLoader);

        if (!classes.iterator().hasNext()) {
            return;
        }

        PreBuildContext.initialize(appModelResolver, buildDir,
                sourcesDir, testSourcesDir,
                compileSourceRegistrar, testSourceRegistrar);

        for (Class<?> clazz : classes) {
            triggerPreBuildSteps(preBuildStepClass, clazz);
        }
    }

    private static void triggerPreBuildSteps(Class<? extends Annotation> preBuildStepClass, Class<?> clazz) {
        final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        if (constructors.length != 1) {
            throw new PreBuildException("PreBuildStep classes must have exactly one constructor");
        }
        Constructor<?> constructor = constructors[0];
        if (!(Modifier.isPublic(constructor.getModifiers())))
            constructor.setAccessible(true);
        if (constructor.getParameters().length > 0) {
            throw new PreBuildException("PreBuildStep classes must have a no-arg constructor");
        }

        Object preBuildStepObject;
        try {
            preBuildStepObject = constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new PreBuildException("Failed to create pre build step class " + clazz, e);
        }

        final Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            final int mods = method.getModifiers();
            if (Modifier.isStatic(mods)) {
                continue;
            }
            if (!method.isAnnotationPresent(preBuildStepClass))
                continue;
            if (!Modifier.isPublic(mods) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                method.setAccessible(true);
            }
            try {
                method.invoke(preBuildStepObject);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new PreBuildException("Invoking pre build step " + method + " failed", e);
            }
        }
    }

    private static Iterable<Class<?>> getPreBuildClasses(ClassLoader deploymentClassLoader) {
        try {
            return ServiceUtil.classesNamedIn(deploymentClassLoader,
                    "META-INF/quarkus-pre-build-steps.list");
        } catch (IOException | ClassNotFoundException e) {
            throw new PreBuildException("Failed to load pre-build step classes list", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> getPreBuildStepClass(ClassLoader deploymentClassLoader) {
        try {
            return (Class<? extends Annotation>) deploymentClassLoader
                    .loadClass(PreBuildStep.class.getName());
        } catch (ClassNotFoundException e) {
            throw new PreBuildException("Failed to find " + PreBuildStep.class + " in the deployemnt classloader", e);
        }
    }
}
