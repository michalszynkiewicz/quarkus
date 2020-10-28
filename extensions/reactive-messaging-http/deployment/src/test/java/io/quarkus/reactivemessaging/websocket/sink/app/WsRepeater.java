package io.quarkus.reactivemessaging.websocket.sink.app;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.quarkus.reactivemessaging.http.runtime.WebsocketMessage;
import io.vertx.core.buffer.Buffer;

@ApplicationScoped
public class WsRepeater {

    @Incoming("post-http-source")
    @Outgoing("my-ws-sink")
    WebsocketMessage passThrough(HttpMessage message) {
        return new WebsocketMessage(Buffer.buffer("{\"foo\": \"bar\"}"));
    }
}
