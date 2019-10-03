package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.jboss.jandex.DotName;

/**
 * Allows to programatically register additional interceptor bindings.
 */
public interface InterceptorBindingRegistrar extends BuildExtension {

    /**
     * Annotations in a form of {@link DotName} to be considered interceptor bindings.
     * Optionally, mapped to a {@link Collection} of non-binding fields
     */
    // mstodo may be better to provide a custom structure with clearer responsibilities
    Map<DotName, Set<String>> registerAdditionalBindings();
}
