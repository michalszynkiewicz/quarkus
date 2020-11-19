package io.quarkus.reactivemessaging.http.runtime.config;

public class WebsocketStreamConfig {
    public final String path;
    public final int bufferSize;

    public WebsocketStreamConfig(String path, int bufferSize) {
        this.path = path;
        this.bufferSize = bufferSize;
    }

    public String path() {
        return path;
    }
}
