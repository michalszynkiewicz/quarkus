package io.quarkus.it.cdi.security;

import javax.annotation.security.DenyAll;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 16/10/2019
 */
@ApplicationScoped
public class PermitAllSubBean {
    public String unannotatedInSubclass() {
        return "unannotatedInSubclass";
    }

    @DenyAll
    public String deniedInSubclass() {
        return "deniedInSubclass";
    }
}
