package io.quarkus.it.cdi.security;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@PermitAll
public class PermitAllBean {

    public String unannotated() {
        return "unannotated";
    }

    @DenyAll
    public String forbidden() {
        return "forbidden";
    }
}
