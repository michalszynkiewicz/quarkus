package io.quarkus.security.deployment;

import javax.annotation.security.DenyAll;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 11/10/2019
 */
public class SecurityTransformerUtils {
    public static final DotName DENY_ALL = DotName.createSimple(DenyAll.class.getName());

    public static boolean hasSecurityAnnotation(MethodInfo methodInfo) {
        return methodInfo.annotations().stream()
                .map(AnnotationInstance::name)
                .anyMatch(SecurityAnnotationsRegistrar.SECURITY_BINDINGS.keySet()::contains);
    }

    public static boolean hasSecurityAnnotation(ClassInfo classInfo) {
        return classInfo.classAnnotations().stream()
                .map(AnnotationInstance::name)
                .anyMatch(SecurityAnnotationsRegistrar.SECURITY_BINDINGS.keySet()::contains);
    }
}
