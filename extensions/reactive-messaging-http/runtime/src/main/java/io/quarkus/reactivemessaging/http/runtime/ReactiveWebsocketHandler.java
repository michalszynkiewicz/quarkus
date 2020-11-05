package io.quarkus.reactivemessaging.http.runtime;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class ReactiveWebsocketHandler implements Handler<RoutingContext> {
    private final ReactiveHttpHandlerBean handler;

    ReactiveWebsocketHandler(ReactiveHttpHandlerBean handler) {
        this.handler = handler;
    }

    @Override
    public void handle(RoutingContext event) {
        handler.handleWebsocket(event);
    }
}
