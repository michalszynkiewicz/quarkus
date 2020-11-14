package io.quarkus.reactivemessaging.http.runtime.config;

import io.vertx.core.http.HttpMethod;

public class HttpStreamConfig {
    public final HttpMethod method;
    public final String path;
    public final Integer bufferSize;

    public HttpStreamConfig(String path, String method, String name, int bufferSize) {
        this.path = path;
        this.method = toHttpMethod(method, name);
        this.bufferSize = bufferSize;
    }

    public String path() {
        return path;
    }

    private HttpMethod toHttpMethod(String method, String connectorName) {
        try {
            return HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid http method '" + method + "' defined for connector " + connectorName);
        }
    }
}
