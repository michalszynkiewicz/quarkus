package io.quarkus.resteasy.reactive.client.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.MethodCreator;

public interface JaxrsClientEnricher {
    void enrichGlobalWebTarget(MethodCreator ctor, AssignableResultHandle globalTarget, ClassInfo interfaceClass,
            IndexView index);

    void enrichMethodWebTarget(MethodCreator methodCreator, ClassInfo interfaceClass, MethodInfo method,
            AssignableResultHandle methodWebTarget, IndexView index, BuildProducer<GeneratedClassBuildItem> generatedClasses,
            int methodIndex);
}
