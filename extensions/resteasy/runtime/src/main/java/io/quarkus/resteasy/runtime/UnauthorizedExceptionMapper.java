package io.quarkus.resteasy.runtime;

import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.core.ResteasyContext;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.UnauthorizedException;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 05/10/2019
 */
@Provider
@Priority(Priorities.USER + 1)
public class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {

    //Servlet API may not be present
    private static final Class<?> HTTP_SERVLET_REQUEST;
    private static final Class<?> HTTP_SERVLET_RESPONSE;
    private static final Method AUTHENTICATE;

    static {
        Class<?> httpServletReq = null;
        Class<?> httpServletResp = null;
        Method auth = null;
        try {
            httpServletReq = Class.forName("javax.servlet.http.HttpServletRequest");
            httpServletResp = Class.forName("javax.servlet.http.HttpServletResponse");
            auth = httpServletReq.getMethod("authenticate", httpServletResp);
        } catch (Exception e) {

        }
        AUTHENTICATE = auth;
        HTTP_SERVLET_REQUEST = httpServletReq;
        HTTP_SERVLET_RESPONSE = httpServletResp;
    }

    @Override
    public Response toResponse(UnauthorizedException exception) {
        SecurityIdentity identity = CurrentIdentityAssociation.current();
        if (HTTP_SERVLET_REQUEST != null) {
            Object httpServletRequest = ResteasyContext.getContextData(HTTP_SERVLET_REQUEST);
            if (httpServletRequest != null) {
                Object httpServletResponse = ResteasyContext.getContextData(HTTP_SERVLET_RESPONSE);
                try {
                    AUTHENTICATE.invoke(httpServletRequest, httpServletResponse);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        HttpAuthenticator authenticator = identity.getAttribute(HttpAuthenticator.class.getName());
        RoutingContext context = ResteasyContext.getContextData(RoutingContext.class);
        if (authenticator != null && context != null) {
            return Response.status(authenticator.challengeStatus())
                    .header(authenticator.challengeHeader().toString(), authenticator.challengeContent())
                    .build();
        } else {
            return Response.status(401).entity("Not authorized").build();
        }
    }
}
