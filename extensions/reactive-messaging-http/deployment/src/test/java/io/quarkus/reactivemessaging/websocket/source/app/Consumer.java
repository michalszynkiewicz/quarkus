package io.quarkus.reactivemessaging.websocket.source.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkus.reactivemessaging.VertxFriendlyLock;
import io.vertx.core.Vertx;

@ApplicationScoped
public class Consumer {

    private final List<String> messages = new ArrayList<>();

    VertxFriendlyLock lock;

    @Inject
    Consumer(Vertx vertx) {
        lock = new VertxFriendlyLock(vertx);
    }

    @Incoming("my-ws-source")
    public CompletionStage<Void> process(Message<String> message) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        lock.triggerWhenUnlocked(() -> {
            messages.add(message.getPayload());
            message.ack();
            result.complete(null);
        }, 10000);
        return result;
    }

    @Incoming("my-ws-source-buffer-13")
    public CompletionStage<Void> processWithBuffer13(Message<String> message) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        lock.triggerWhenUnlocked(() -> {
            messages.add(message.getPayload());
            message.ack();
            result.complete(null);
        }, 10000);
        return result;
    }

    public List<String> getMessages() {
        // mstodo deserializers
        // mstodo test deserializers
        return messages;
    }

    public void pause() {
        lock.lock();
    }

    public void resume() {
        lock.unlock();
    }

    public void clear() {
        messages.clear();
        lock.reset();
    }
}
