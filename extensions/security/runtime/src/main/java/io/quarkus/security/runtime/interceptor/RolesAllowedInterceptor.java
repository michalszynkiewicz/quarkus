package io.quarkus.security.runtime.interceptor;

import javax.annotation.Priority;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 03/10/2019
 */
@Interceptor
@RolesAllowed("")
@Priority(1)
public class RolesAllowedInterceptor {

    @Inject
    SecurityHandler handler;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        return handler.handle(ic);
    }
}
