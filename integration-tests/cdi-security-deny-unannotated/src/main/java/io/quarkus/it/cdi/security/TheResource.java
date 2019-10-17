package io.quarkus.it.cdi.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Path("/api")
public class TheResource {
    @Inject
    @Named(BeanWithSecurityAnnotations.NAME)
    BeanWithSecurityAnnotations beanWithSecurityAnnotations;

    @Inject
    BeanWithSecurityAnnotationsSubBean securityAnnoSubBean;

    @Inject
    PermitAllSubBean permitAllSubBean;

    @Inject
    BeanWithNoSecurityAnnotations noAnnoBean;

    @GET
    @Path("/unannotatedMethod")
    public String unannotatedMethod() {
        return beanWithSecurityAnnotations.unannotated();
    }

    @GET
    @Path("/allowedMethod")
    public String allowedMethod() {
        return beanWithSecurityAnnotations.allowed();
    }

    @GET
    @Path("/unannotatedInPermitAllSubclass")
    public String unannotatedInPermitAllSubclass() {
        return permitAllSubBean.unannotatedInSubclass();
    }

    @GET
    @Path("/restrictedMethod")
    public String restricted() {
        return beanWithSecurityAnnotations.restricted();
    }

    @GET
    @Path("/unannotatedOnBeanWithNoAnno")
    public String unannotatedOnBeanWithNoAnno() {
        return noAnnoBean.unannotated();
    }

    @GET
    @Path("/inheritedDeniedByDefault")
    public String inheritedDeniedByDefault() {
        return securityAnnoSubBean.unannotated();
    }

}
