package io.quarkus.bootstrap.prebuild;

/**
 * PreBuildStep exception that translates to MojoFailureException (instead of MojoExecutionException)
 */
public class PreBuildFailureException extends PreBuildException {
    public PreBuildFailureException(String message) {
        super(message);
    }

    public PreBuildFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public PreBuildFailureException(Throwable cause) {
        super(cause);
    }

    public PreBuildFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
