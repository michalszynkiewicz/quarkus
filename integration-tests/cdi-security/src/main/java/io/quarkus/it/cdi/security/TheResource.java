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
    @Named(BeanWithSecuredMethods.NAME)
    BeanWithSecuredMethods bean;

    @Inject
    @Named(SubclassWithDenyAll.NAME)
    SubclassWithDenyAll denyAllBean;

    @Inject
    SubclassWithPermitAll permitAllBean;

    @Inject
    SubclassWithoutAnnotations unannotatedBean;

    @GET
    public String freeToAccess() {
        return "here you go!";
    }

    @GET
    @Path("/forbiddenMethod")
    public String forbiddenMethod() {
        return bean.forbidden();
    }

    @GET
    @Path("/allowedMethod")
    public String allowedMethod() {
        return bean.unsecuredMethod();
    }

    @GET
    @Path("/securedMethod")
    public String securedMethod() {
        return bean.securedMethod();
    }

    @GET
    @Path("/forbiddenOnClass")
    public String forbiddenOnClass() {
        return denyAllBean.noAdditionalConstraints();
    }

    @GET
    @Path("/allowedMethodOnForbiddenClass")
    public String allowedOnForbiddenClass() {
        return denyAllBean.allowedMethod();
    }

    @GET
    @Path("/securedMethodOnForbiddenClass")
    public String securedOnForbiddenClass() {
        return denyAllBean.restrictedOnMethod();
    }

    @GET
    @Path("/inheritedAllowedMethod")
    public String inheritedAllowedMethod() {
        return denyAllBean.unsecuredMethod();
    }

    @GET
    @Path("/inheritedForbiddenMethod")
    public String inheritedForbiddenMethod() {
        return denyAllBean.forbidden();
    }

    @GET
    @Path("/allowedOnClass")
    public String allowedOnClass() {
        return permitAllBean.allowedOnClass();
    }

    @GET
    @Path("/forbiddenMethodOnFreeAccessClass")
    public String forbiddenOnMethodOnPermitAllClass() {
        return permitAllBean.forbiddenOnMethod();
    }

    @GET
    @Path("/restrictedMethodOnFreeAccessClass")
    public String restrictedOnMethodOnPermitAllClass() {
        return permitAllBean.restrictedOnMethod();
    }

    @GET
    @Path("/inheritedAllowedMethodOnPermitAllClass")
    public String inheritedAllowedMethodOnPermitAll() {
        return permitAllBean.unsecuredMethod();
    }

    @GET
    @Path("/inheritedForbiddenMethodOnPermitAllClass")
    public String inheritedForbiddenMethodOnPermitAll() {
        return permitAllBean.forbidden();
    }

    @GET
    @Path("/inheritedForbiddenOnUnannotatedClass")
    public String inheritedForbiddenOnUnannotatedClass() {
        return unannotatedBean.noAdditionalConstraints();
    }
}
