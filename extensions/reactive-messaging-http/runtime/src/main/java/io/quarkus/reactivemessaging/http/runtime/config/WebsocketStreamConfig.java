package io.quarkus.reactivemessaging.http.runtime.config;

public class WebsocketStreamConfig {
    public final String path;

    public WebsocketStreamConfig(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
