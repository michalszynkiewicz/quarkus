package io.quarkus.reactivemessaging.http.sink.app;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class HttpRepeater {

    public static final String HEADER_NAME = "http-header-from-repeater";
    public static final String HEADER_VALUE = "headerValue";

    // mstodo add a test for serialized json

    @Incoming("post-http-source")
    @Outgoing("my-http-sink")
    HttpMessage<JsonObject> passThrough(HttpMessage<Buffer> message) {
        HttpMessage<JsonObject> newMessage = new HttpMessage<>(new JsonObject(message.getPayload()),
                MultiMap.caseInsensitiveMultiMap().add(HEADER_NAME, HEADER_VALUE));
        return newMessage;
    }

    @Incoming("custom-http-source")
    @Outgoing("custom-http-sink")
    HttpMessage<String> passThroughWithCustomSerializer(HttpMessage<Buffer> message) {
        HttpMessage<String> newMessage = new HttpMessage<>(message.getPayload().toString(),
                MultiMap.caseInsensitiveMultiMap().add(HEADER_NAME, HEADER_VALUE));
        return newMessage;
    }
}
