package io.quarkus.security.runtime.interceptor;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.quarkus.security.Authenticated;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 08/10/2019
 */
@Interceptor
@Authenticated
@Priority(1)
public class AuthenticatedInterceptor {

    @Inject
    SecurityHandler handler;

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        return handler.handle(ic);
    }
}
