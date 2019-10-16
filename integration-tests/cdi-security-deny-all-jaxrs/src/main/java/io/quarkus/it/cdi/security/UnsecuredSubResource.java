package io.quarkus.it.cdi.security;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 11/10/2019
 */
public class UnsecuredSubResource {
    @GET
    @Path("/subMethod")
    public String subMethod() {
        return "subMethod";
    }
}
