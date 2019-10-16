package io.quarkus.resteasy.runtime;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.quarkus.security.runtime.ForbiddenException;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 05/10/2019
 */
@Provider
@Priority(Priorities.USER + 1)
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {
    @Override
    public Response toResponse(ForbiddenException exception) {
        return Response.status(403).entity("Access forbidden: role not allowed").build();
    }
}
