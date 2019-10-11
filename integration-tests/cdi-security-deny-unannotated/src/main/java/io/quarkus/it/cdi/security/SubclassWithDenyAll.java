package io.quarkus.it.cdi.security;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 04/10/2019
 */
@DenyAll
@ApplicationScoped
@Named(SubclassWithDenyAll.NAME)
public class SubclassWithDenyAll extends BeanWithSecuredMethods {

    public static final String NAME = "SubclassWithDenyAll";

    public String noAdditionalConstraints() {
        return "forbiddenOnClass";
    }

    @PermitAll
    public String allowedMethod() {
        return "allowedOnMethod";
    }

    @RolesAllowed("admin")
    public String restrictedOnMethod() {
        return "restrictedOnMethod";
    }
}
