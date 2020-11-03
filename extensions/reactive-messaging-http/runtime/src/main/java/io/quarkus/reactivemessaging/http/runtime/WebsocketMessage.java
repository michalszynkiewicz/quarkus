package io.quarkus.reactivemessaging.http.runtime;

import org.eclipse.microprofile.reactive.messaging.Message;

public class WebsocketMessage<PayloadType> implements Message<PayloadType> {

    private final PayloadType payload;

    public WebsocketMessage(PayloadType payload) {
        this.payload = payload;
    }

    @Override
    public PayloadType getPayload() {
        return payload;
    }
}
