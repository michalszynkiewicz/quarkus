package io.quarkus.reactivemessaging.http;

import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 09/07/2019
 */
public class HttpMessage<T> implements Message<T> {

    final T payload;

    public HttpMessage(T content) {
        payload = content;
    }

    @Override
    public T getPayload() {
        return payload;
    }
}
