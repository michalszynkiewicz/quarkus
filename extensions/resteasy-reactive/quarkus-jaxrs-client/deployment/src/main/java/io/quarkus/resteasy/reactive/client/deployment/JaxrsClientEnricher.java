package io.quarkus.resteasy.reactive.client.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;

public interface JaxrsClientEnricher {
    void enrichWebTarget(MethodCreator ctor, ResultHandle res, ClassInfo interfaceClass, IndexView index);
}
