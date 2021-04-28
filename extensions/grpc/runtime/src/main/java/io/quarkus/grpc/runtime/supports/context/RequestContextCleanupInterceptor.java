package io.quarkus.grpc.runtime.supports.context;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.ManagedContext;
import org.jboss.logging.Logger;

@Interceptor
@CleanUpRequestContext
public class RequestContextCleanupInterceptor {
    private static final Logger log = Logger.getLogger(RequestContextCleanupInterceptor.class);

    @AroundInvoke
    public Object cleanUpContext(InvocationContext invocationContext) throws Exception {
        boolean cleanUp = false;
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            InstanceHandle<GrpcRequestContextMarkerBean> marker = Arc.container()
                    .instance(GrpcRequestContextMarkerBean.class);
            cleanUp = marker.get().isCreatedWithGrpc();
        }
        try {
            return invocationContext.proceed();
        } finally {
            if (cleanUp && requestContext.isActive()) {
                log.infov("deactivating for {0}", Thread.currentThread().toString());
                requestContext.deactivate();
            }
        }
    }
}
