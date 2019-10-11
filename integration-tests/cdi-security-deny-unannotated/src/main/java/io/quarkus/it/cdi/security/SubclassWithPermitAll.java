package io.quarkus.it.cdi.security;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 04/10/2019
 */
@PermitAll
@ApplicationScoped
public class SubclassWithPermitAll extends BeanWithSecuredMethods {
    public String allowedOnClass() {
        return "allowedOnClass";
    }

    @DenyAll
    public String forbiddenOnMethod() {
        return "forbiddenOnMethod";
    }

    @RolesAllowed("admin")
    public String restrictedOnMethod() {
        return "restrictedOnMethod";
    }
}
