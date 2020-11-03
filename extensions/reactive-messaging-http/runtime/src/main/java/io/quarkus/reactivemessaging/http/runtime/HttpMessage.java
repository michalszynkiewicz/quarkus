package io.quarkus.reactivemessaging.http.runtime;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.vertx.core.MultiMap;

/**
 * used by http source
 * 
 * @param <T> payload type
 */
public class HttpMessage<T> implements Message<T> {

    private final T payload;
    private final MultiMap httpHeaders;

    public HttpMessage(T payload, MultiMap httpHeaders) {
        this.payload = payload;
        this.httpHeaders = httpHeaders;
    }

    @Override
    public T getPayload() {
        return payload;
    }

    public MultiMap getHttpHeaders() {
        return httpHeaders;
    }
}
