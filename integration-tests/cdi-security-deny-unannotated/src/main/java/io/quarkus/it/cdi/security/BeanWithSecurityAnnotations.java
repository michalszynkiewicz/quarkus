package io.quarkus.it.cdi.security;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 16/10/2019
 */
@ApplicationScoped
@Named(BeanWithSecurityAnnotations.NAME)
public class BeanWithSecurityAnnotations {
    public static final String NAME = "BeanWithSecurityAnnotations";

    public String unannotated() {
        return "unannotated";
    }

    @PermitAll
    public String allowed() {
        return "allowed";
    }

    @RolesAllowed("admin")
    public String restricted() {
        return "accessibleForAdminOnly";
    }
}
