package io.quarkus.it.cdi.security;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 16/10/2019
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
