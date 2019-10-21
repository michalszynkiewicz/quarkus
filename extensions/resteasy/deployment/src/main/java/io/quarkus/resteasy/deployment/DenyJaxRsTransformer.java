package io.quarkus.resteasy.deployment;

import static io.quarkus.security.deployment.SecurityTransformerUtils.DENY_ALL;
import static io.quarkus.security.deployment.SecurityTransformerUtils.hasSecurityAnnotation;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.resteasy.spi.ResteasyDeployment;

import io.quarkus.arc.processor.AnnotationsTransformer;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class DenyJaxRsTransformer implements AnnotationsTransformer {
    private final ResteasyDeployment resteasyDeployment;

    public DenyJaxRsTransformer(ResteasyDeployment resteasyDeployment) {
        this.resteasyDeployment = resteasyDeployment;
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
        String className = classInfo.name().toString();
        return resteasyDeployment.getScannedResourceClasses().contains(className);
    }
}
