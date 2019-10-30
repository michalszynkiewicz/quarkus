package io.quarkus.security.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public final class SecuredMethodBuildItem extends MultiBuildItem {
    private final MethodInfo methodInfo;
    private final String[] value;
    private final DotName annotationClass;

    public SecuredMethodBuildItem(MethodInfo methodInfo, AnnotationInstance annotationInstance) {
        this.methodInfo = methodInfo;
        this.annotationClass = annotationInstance.name();
        this.value = annotationInstance.value() != null ? annotationInstance.value().asStringArray() : null;
    }

    public SecuredMethodBuildItem(MethodInfo methodInfo, DotName annotationClass, String[] value) {
        this.methodInfo = methodInfo;
        this.value = value;
        this.annotationClass = annotationClass;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public DotName annotationClass() {
        return annotationClass;
    }

    public String[] value() {
        return value;
    }
}
