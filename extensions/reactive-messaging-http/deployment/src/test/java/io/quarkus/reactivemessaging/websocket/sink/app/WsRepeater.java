package io.quarkus.reactivemessaging.websocket.sink.app;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.quarkus.reactivemessaging.http.runtime.WebsocketMessage;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class WsRepeater {

    public static final String BUFFER = "BUFFER";
    public static final String JSON_OBJECT = "JSON_OBJECT";
    public static final String JSON_ARRAY = "JSON_ARRAY";
    public static final String STRING = "STRING";

    @Incoming("post-http-source")
    @Outgoing("my-ws-sink")
    WebsocketMessage<?> passThrough(HttpMessage message) {
        try {
            switch (message.getPayload().toString()) {
                case BUFFER:
                    return new WebsocketMessage<>(Buffer.buffer("{\"foo\": \"bar\"}"));
                case JSON_OBJECT:
                    return new WebsocketMessage<>(new JsonObject().put("jsonFoo", "jsonBar"));
                case JSON_ARRAY:
                    return new WebsocketMessage<>(new JsonArray().add(new JsonObject().put("arrFoo", "arrBar")));
                case STRING:
                    return new WebsocketMessage<>("someText");
                default:
                    throw new IllegalArgumentException("Unexpected payload: " + message.getPayload().toString());
            }
        } finally {
            message.ack();
        }
    }
}
