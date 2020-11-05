package io.quarkus.reactivemessaging.http.runtime;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class ReactiveHttpHandler implements Handler<RoutingContext> {
    private final ReactiveHttpHandlerBean handler;

    ReactiveHttpHandler(ReactiveHttpHandlerBean handler) {
        this.handler = handler;
    }

    @Override
    public void handle(RoutingContext event) {
        handler.handleHttp(event);
    }
}
