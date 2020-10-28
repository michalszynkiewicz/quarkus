package io.quarkus.reactivemessaging.http.sink.app;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;

public class HttpRepeater {

    public static final String HEADER_NAME = "http-header-from-repeater";
    public static final String HEADER_VALUE = "headerValue";

    @Incoming("post-http-source")
    @Outgoing("my-http-sink")
    HttpMessage passThrough(HttpMessage message) {
        HttpMessage newMessage = new HttpMessage(Buffer.buffer(
                "{\"foo\": \"bar\"}"), // mstodo change to serialized json
                MultiMap.caseInsensitiveMultiMap().add(HEADER_NAME, HEADER_VALUE));
        return newMessage;
    }
}
