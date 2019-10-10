package io.quarkus.it.cdi.security;

import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 04/10/2019
 */
@ApplicationScoped
@Named(BeanWithSecuredMethods.NAME)
public class BeanWithSecuredMethods {
    public static final String NAME = "super-bean";

    @DenyAll
    public String forbidden() {
        return "shouldBeDenied";
    }

    @RolesAllowed("admin")
    public String securedMethod() {
        return "accessibleForAdminOnly";
    }

    public String unsecuredMethod() {
        return "accessibleForAll";
    }
}
