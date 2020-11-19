package io.quarkus.reactivemessaging.http.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@Recorder
public class ReactiveHttpRecorder {

    public Handler<RoutingContext> createWebsocketHandler() {
        ReactiveWebsocketHandlerBean bean = Arc.container().instance(ReactiveWebsocketHandlerBean.class).get();
        return new ReactiveWebsocketHandler(bean);
    }

    public Handler<RoutingContext> createHttpHandler() {
        ReactiveHttpHandlerBean bean = Arc.container().instance(ReactiveHttpHandlerBean.class).get();
        return new ReactiveHttpHandler(bean);
    }

    public Handler<RoutingContext> createBodyHandler() {
        return BodyHandler.create();
    }
}
