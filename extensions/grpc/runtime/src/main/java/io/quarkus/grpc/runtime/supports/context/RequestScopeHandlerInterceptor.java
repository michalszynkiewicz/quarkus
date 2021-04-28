package io.quarkus.grpc.runtime.supports.context;

import org.jboss.logmanager.Logger;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class RequestScopeHandlerInterceptor implements ServerInterceptor {

    private final ManagedContext reqContext;
    private static final Logger LOGGER = Logger.getLogger(RequestScopeHandlerInterceptor.class.getName());

    public RequestScopeHandlerInterceptor() {
        reqContext = Arc.container().requestContext();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // This interceptor is called first, so, we should be on the event loop.
        Context capturedVertxContext = Vertx.currentContext();
        if (capturedVertxContext != null) {
            InjectableContext.ContextState state;
            boolean activateAndDeactivateContext = !reqContext.isActive();
            if (activateAndDeactivateContext) {
                reqContext.activate();
                GrpcRequestContextMarkerBean marker = Arc.container().instance(GrpcRequestContextMarkerBean.class).get();
                marker.setCreatedWithGrpc(true);
                state = reqContext.getState();
            } else {
                state = null;
            }
            return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                @Override
                public void close(Status status, Metadata trailers) {
                    super.close(status, trailers);
                    if (activateAndDeactivateContext) {
                        capturedVertxContext.runOnContext(new Handler<Void>() {
                            @Override
                            public void handle(Void ignored) {
                                reqContext.destroy(state);
                            }
                        });
                    }
                }
            }, headers);
        } else {
            LOGGER.warning("Unable to activate the request scope - interceptor not called on the Vert.x event loop");
            return next.startCall(call, headers);
        }
    }

}
