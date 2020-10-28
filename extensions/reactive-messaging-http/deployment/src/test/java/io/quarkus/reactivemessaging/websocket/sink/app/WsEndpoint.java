package io.quarkus.reactivemessaging.websocket.sink.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.reactivemessaging.http.runtime.WebsocketMessage;

public class WsEndpoint {
    private final List<WebsocketMessage> messages = new ArrayList<>();

    @Incoming("my-ws-source")
    public CompletionStage<Void> process(WebsocketMessage message) {
        messages.add(message);
        CompletableFuture<Void> result = new CompletableFuture<>();
        result.complete(null);
        return result;
    }
}
