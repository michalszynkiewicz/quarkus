package io.quarkus.maven;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "prepare-tests", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PreBuildTestMojo extends PreBuildMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String projectDir = project.getBasedir().getAbsolutePath();
        Path testSources = Paths.get(projectDir, "src", "test");
        doExecute(testSources, path -> project.addTestCompileSourceRoot(path.toString()), true);
    }
}
