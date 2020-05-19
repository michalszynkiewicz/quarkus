package io.quarkus.gradle.tasks;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;

import io.quarkus.bootstrap.prebuild.PreBuildException;
import org.gradle.api.GradleException;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.deployment.PreBuilder;

public class QuarkusPrepare extends QuarkusTask {

    private Path sourcesDirectory;
    private Path testSourcesDirectory;
    private Consumer<Path> testSourceRegistrar;
    private Consumer<Path> sourceRegistrar;

    public QuarkusPrepare() {
        super("Quarkus performs pre-build preparations, such as sources generation");
    }

    @TaskAction
    public void prepareQuarkus() {
        getLogger().lifecycle("preparing quarkus application");

        final AppArtifact appArtifact = extension().getAppArtifact();
        appArtifact.setPaths(QuarkusGradleUtils.getOutputPaths(getProject()));
        File fakeAppArtifact = getTemporaryDirFactory().create();
        appArtifact.setPath(fakeAppArtifact.toPath());

        final AppModelResolver modelResolver = extension().getAppModelResolver();

        final Properties realProperties = getBuildSystemProperties(appArtifact);

        try (CuratedApplication appCreationContext = QuarkusBootstrap.builder() // mstodo share it with build task ?
                .setBaseClassLoader(getClass().getClassLoader())
                .setAppModelResolver(modelResolver)
                .setTargetDirectory(getProject().getBuildDir().toPath())
                .setBaseName(extension().finalName())
                .setBuildSystemProperties(realProperties)
                .setAppArtifact(appArtifact)
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                .build().bootstrap()) {

            final Convention convention = getProject().getConvention();
            JavaPluginConvention javaConvention = convention.findPlugin(JavaPluginConvention.class);
            if (javaConvention != null) {
                final SourceSet generatedSources = javaConvention.getSourceSets().create("generated-sources");
                generatedSources.getOutput().dir("generated-sources");
                PathsCollection.Builder paths = PathsCollection.builder();
                generatedSources.getOutput().filter(File::exists).forEach(f -> {
                    paths.add(f.toPath());
                });
                appArtifact.setPaths(paths.build());
            }
            getLogger().info("Will trigger preparing sources for source directory: {} buildDir: {}",
                    sourcesDirectory.toAbsolutePath(), getProject().getBuildDir().getAbsolutePath());
            PreBuilder.prepareSources(appCreationContext.createDeploymentClassLoader(),
                    getProject().getBuildDir().toPath(),
                    sourcesDirectory,
                    testSourcesDirectory,
                    modelResolver,
                    sourceRegistrar,
                    testSourceRegistrar);
        } catch (BootstrapException e) {
            throw new GradleException("Failed to generate sources in the QuarkusPrepare task", e);
        } catch (PreBuildException e) {
            throw new GradleException("Failed to generate sources in the QuarkusPrepare task", e);
        }
    }

    public void setSourcesDirectory(Path sourcesDirectory) {
        this.sourcesDirectory = sourcesDirectory;
    }

    public void setTestSourcesDirectory(Path testSourcesDirectory) {
        this.testSourcesDirectory = testSourcesDirectory;
    }

    public void setTestSourceRegistrar(Consumer<Path> testSourceRegistrar) {
        this.testSourceRegistrar = testSourceRegistrar;
    }

    public void setSourceRegistrar(Consumer<Path> sourceRegistrar) {
        this.sourceRegistrar = sourceRegistrar;
    }
}
