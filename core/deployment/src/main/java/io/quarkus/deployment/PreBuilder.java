package io.quarkus.deployment;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.function.Consumer;

import io.quarkus.bootstrap.PreBuildContext;
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
            Consumer<Path> testSourceRegistrar) throws IOException, ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        Thread.currentThread().setContextClassLoader(deploymentClassLoader);
        // mstodo do not allow to have build steps and pre-build steps in the same class?

        // gather pre-build steps
        Iterable<Class<?>> classes = ServiceUtil.classesNamedIn(deploymentClassLoader,
                "META-INF/quarkus-pre-build-steps.list");
        // mstodo: we declare some constructors programmatically, check out that code
        if (!classes.iterator().hasNext()) {
            return;
        }

        PreBuildContext.initialize(appModelResolver, buildDir,
                sourcesDir, testSourcesDir,
                compileSourceRegistrar, testSourceRegistrar);

        for (Class<?> clazz : classes) {
            final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            if (constructors.length != 1) {
                throw new RuntimeException("PreBuildStep classes must have exactly one constructor"); // mstodo pretty error reporting
            }
            Constructor<?> constructor = constructors[0];
            if (!(Modifier.isPublic(constructor.getModifiers())))
                constructor.setAccessible(true);
            if (constructor.getParameters().length > 0) {
                throw new RuntimeException("PreBuildStep classes must have a no-arg constructor"); // mstodo pretty error reporting
            }

            Object preBuildStepObject = clazz.getDeclaredConstructor().newInstance();

            final Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                final int mods = method.getModifiers();
                if (Modifier.isStatic(mods)) {
                    continue;
                }
                if (!method.isAnnotationPresent(PreBuildStep.class))
                    continue;
                if (!Modifier.isPublic(mods) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                    method.setAccessible(true);
                }
                // mstodo support method parameters?
                method.invoke(preBuildStepObject);
            }
        }
    }
}
