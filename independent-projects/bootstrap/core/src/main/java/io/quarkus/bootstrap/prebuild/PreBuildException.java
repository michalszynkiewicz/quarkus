package io.quarkus.bootstrap.prebuild;

/**
 * An exception thrown by the PreBuildSteps.
 * Translates to maven's MojoExecutionException
 */
public class PreBuildException extends RuntimeException {
    public PreBuildException(String message) {
        super(message);
    }

    public PreBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreBuildException(Throwable cause) {
        super(cause);
    }

    public PreBuildException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
