package io.quarkus.resteasy.deployment;

import static io.quarkus.security.deployment.SecurityTransformerUtils.DENY_ALL;
import static io.quarkus.security.deployment.SecurityTransformerUtils.hasSecurityAnnotation;

import java.lang.reflect.Modifier;
import java.util.Objects;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.resteasy.common.deployment.ResteasyDotNames;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 11/10/2019
 */
public class DenyJaxRsTransformer implements AnnotationsTransformer {
    private final IndexView index;

    public DenyJaxRsTransformer(IndexView index) {
        this.index = index;
    }

    @Override
    public boolean appliesTo(AnnotationTarget.Kind kind) {
        return kind == AnnotationTarget.Kind.CLASS;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        ClassInfo classInfo = transformationContext.getTarget().asClass();
        if (!hasSecurityAnnotation(classInfo) && isJaxRsResource(classInfo)) {
            transformationContext.transform().add(DENY_ALL).done();
        }
    }

    private boolean isJaxRsResource(ClassInfo classInfo) {
        // mstodo subresources - do they need special handling?
        return !Modifier.isInterface(classInfo.flags())
                && (implementsJaxRsInterface(classInfo) || hasPathAnnotation(classInfo));
    }

    private boolean hasPathAnnotation(ClassInfo classInfo) {
        return classInfo.classAnnotation(ResteasyDotNames.PATH) != null;
    }

    private boolean implementsJaxRsInterface(ClassInfo classInfo) {
        return classInfo.interfaceNames().stream()
                .map(index::getClassByName)
                .filter(Objects::nonNull)
                .anyMatch(this::hasPathAnnotation);
    }
}
