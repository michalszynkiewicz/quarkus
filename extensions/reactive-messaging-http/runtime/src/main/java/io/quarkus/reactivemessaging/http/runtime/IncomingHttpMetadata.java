package io.quarkus.reactivemessaging.http.runtime;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

// mstodo javadoc
public class IncomingHttpMetadata {

    private final HttpMethod method;
    private final MultiMap headers;
    private final String path;

    IncomingHttpMetadata(HttpServerRequest request) {
        path = request.path();
        headers = request.headers();
        method = request.method();
    }

    public HttpMethod getMethod() {
        return method;
    }

    public MultiMap getHeaders() {
        return headers;
    }

    public String getPath() {
        return path;
    }
}
