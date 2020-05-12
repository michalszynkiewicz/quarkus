package io.quarkus.deployment.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// mstodo: javadoc
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PreBuildStep {
}
