package io.quarkus.reactivemessaging.http.runtime;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.vertx.core.buffer.Buffer;

public class WebsocketMessage implements Message<Buffer> {

    private final Buffer buffer;

    public WebsocketMessage(Buffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public Buffer getPayload() {
        return buffer;
    }
}
