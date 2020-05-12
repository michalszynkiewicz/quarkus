package io.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.PreBuildContext;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.deployment.annotations.PreBuildStep;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.maven.components.MavenVersionEnforcer;

@Mojo(name = "prepare", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PreBuildMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    private File sourceDir;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Component
    private MavenVersionEnforcer mavenVersionEnforcer;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    /** Skip the execution of this mojo */
    @Parameter(defaultValue = "false", property = "quarkus.prepare.skip")
    private boolean skip = false;

    // mstodo copied form build mojo, clean this thing up
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (project.getPackaging().equals("pom")) {
            getLog().info("Type of the artifact is POM, skipping build goal");
            return;
        }
        if (skip) {
            getLog().info("Skipping Quarkus build");
            return;
        }

        try {

            final Properties projectProperties = project.getProperties();
            final Properties realProperties = new Properties();
            for (String name : projectProperties.stringPropertyNames()) {
                if (name.startsWith("quarkus.")) {
                    realProperties.setProperty(name, projectProperties.getProperty(name));
                }
            }
            realProperties.putIfAbsent("quarkus.application.name", project.getArtifactId());
            realProperties.putIfAbsent("quarkus.application.version", project.getVersion());

            MavenArtifactResolver resolver = MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    .build();

            final Artifact projectArtifact = project.getArtifact();
            final AppArtifact appArtifact = new AppArtifact(projectArtifact.getGroupId(), projectArtifact.getArtifactId(),
                    projectArtifact.getClassifier(), projectArtifact.getArtifactHandler().getExtension(),
                    projectArtifact.getVersion());

            CuratedApplication curatedApplication = QuarkusBootstrap.builder()
                    .setAppArtifact(appArtifact)
                    .setProjectRoot(project.getBasedir().toPath())
                    .setMavenArtifactResolver(resolver)
                    .setBaseClassLoader(BuildMojo.class.getClassLoader())
                    .setBuildSystemProperties(realProperties)
                    .setLocalProjectDiscovery(false)
                    .setTargetDirectory(buildDir.toPath())
                    .build().bootstrap();

            QuarkusClassLoader deploymentClassLoader = curatedApplication.createDeploymentClassLoader();

            Thread.currentThread().setContextClassLoader(deploymentClassLoader);
            // mstodo do not allow to have build steps and pre-build steps in the same class?

            // gather pre-build steps
            Iterable<Class<?>> classes = ServiceUtil.classesNamedIn(deploymentClassLoader,
                    "META-INF/quarkus-pre-build-steps.list");
            // mstodo: we declare some constructors programmatically, check out that code
            if (!classes.iterator().hasNext()) {
                return;
            }

            PreBuildContext.initialize(
                    new BootstrapAppModelResolver(resolver),
                    buildDir.toPath(),
                    path -> project.addCompileSourceRoot(path.toString()),
                    path -> project.addTestCompileSourceRoot(path.toString()));

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

                Object preBuildStepObject = clazz.newInstance();

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

        } catch (BootstrapException | AppModelResolverException | IOException | ClassNotFoundException | InstantiationException
                | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failure! ", e); // mstodo handle it the proper maven way!
        }
    }
}
