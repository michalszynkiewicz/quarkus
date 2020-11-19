package io.quarkus.reactivemessaging.http.runtime;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class ReactiveWebsocketHandler implements Handler<RoutingContext> {
    private final ReactiveWebsocketHandlerBean handler;

    ReactiveWebsocketHandler(ReactiveWebsocketHandlerBean handler) {
        this.handler = handler;
    }

    @Override
    public void handle(RoutingContext event) {
        handler.handleWebsocket(event);
    }
}
