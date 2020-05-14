package io.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.deployment.PreBuilder;
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
    private boolean skipSourceGeneration = false;

    // mstodo copied form build mojo, clean this thing up
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (project.getPackaging().equals("pom")) {
            getLog().info("Type of the artifact is POM, skipping build goal");
            return;
        }
        if (skipSourceGeneration) {
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

            final AppArtifact appArtifact = getAppArtifact();

            CuratedApplication curatedApplication = QuarkusBootstrap.builder()
                    .setAppArtifact(appArtifact)
                    .setProjectRoot(project.getBasedir().toPath())
                    .setMavenArtifactResolver(resolver)
                    .setBaseClassLoader(BuildMojo.class.getClassLoader())
                    .setBuildSystemProperties(realProperties)
                    .setLocalProjectDiscovery(false)
                    .setTargetDirectory(buildDir.toPath())
                    .build().bootstrap();

            String projectDir = project.getBasedir().getAbsolutePath();

            PreBuilder.prepareSources(
                    curatedApplication.createDeploymentClassLoader(),
                    buildDir.toPath(),
                    Paths.get(projectDir, "src", "main"), Paths.get(projectDir, "src", "test"), // mstodo we can probably do better
                    new BootstrapAppModelResolver(resolver),
                    path -> project.addCompileSourceRoot(path.toString()),
                    path -> project.addTestCompileSourceRoot(path.toString()));
        } catch (Exception any) {
            throw new MojoFailureException("Prepare phase of the quarkus-maven-plugin failed: " + any.getMessage(), any);
        }
    }

    private AppArtifact getAppArtifact() throws MojoExecutionException {
        final Path classesDir = Paths.get(project.getBuild().getOutputDirectory());
        if (!Files.exists(classesDir)) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Creating empty " + classesDir + " just to be able to resolve the project's artifact");
            }
            try {
                Files.createDirectories(classesDir);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to create " + classesDir);
            }
        }

        final Artifact projectArtifact = project.getArtifact();
        final AppArtifact appArtifact = new AppArtifact(projectArtifact.getGroupId(), projectArtifact.getArtifactId(),
                projectArtifact.getClassifier(), projectArtifact.getArtifactHandler().getExtension(),
                projectArtifact.getVersion());
        appArtifact.setPath(classesDir);
        return appArtifact;
    }
}
