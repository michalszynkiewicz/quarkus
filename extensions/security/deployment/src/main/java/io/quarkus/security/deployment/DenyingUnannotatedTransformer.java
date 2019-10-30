package io.quarkus.security.deployment;

import static io.quarkus.security.deployment.SecurityTransformerUtils.DENY_ALL;
import static io.quarkus.security.deployment.SecurityTransformerUtils.hasSecurityAnnotation;

import java.util.List;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.AnnotationsTransformer;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class DenyingUnannotatedTransformer implements AnnotationsTransformer {

    @Override
    public boolean appliesTo(AnnotationTarget.Kind kind) {
        return kind == AnnotationTarget.Kind.CLASS;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        ClassInfo classInfo = transformationContext.getTarget().asClass();
        if (shouldDenyMethodsByDefault(classInfo)) {
            transformationContext.transform().add(DENY_ALL).done();
        }
    }

    public static boolean shouldDenyMethodsByDefault(ClassInfo classInfo) {
        List<MethodInfo> methods = classInfo.methods();
        return (!hasSecurityAnnotation(classInfo)
                && methods.stream().anyMatch(SecurityTransformerUtils::hasSecurityAnnotation));
    }
}
