package io.quarkus.reactivemessaging.websocket.source.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class Consumer {

    private final List<String> messages = new ArrayList<>();

    @Incoming("my-ws-source")
    public CompletionStage<Void> process(String message) {
        messages.add(message);
        CompletableFuture<Void> result = new CompletableFuture<>();
        result.complete(null);
        return result;
    }

    public List<String> getMessages() { // mstodo deserializers
        return messages;
    }
}
