package io.quarkus.reactivemessaging.http.runtime;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 28/08/2019
 */
public class HttpMessage implements Message<Buffer> {

    private final Buffer buffer;
    private final MultiMap httpHeaders;

    public HttpMessage(Buffer buffer, MultiMap httpHeaders) {
        this.buffer = buffer;
        this.httpHeaders = httpHeaders;
    }

    @Override
    public Buffer getPayload() {
        return buffer;
    }

    public MultiMap getHttpHeaders() {
        return httpHeaders;
    }
}
