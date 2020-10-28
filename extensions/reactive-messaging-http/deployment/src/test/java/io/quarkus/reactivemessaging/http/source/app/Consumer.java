package io.quarkus.reactivemessaging.http.source.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;

@ApplicationScoped
public class Consumer {

    private final List<HttpMessage> postMessages = new ArrayList<>();
    private final List<HttpMessage> putMessages = new ArrayList<>();

    @Incoming("post-http-source")
    public CompletionStage<Void> process(HttpMessage message) {
        postMessages.add(message);
        CompletableFuture<Void> result = new CompletableFuture<>();
        result.complete(null);
        return result;
    }

    @Incoming("put-http-source")
    public CompletionStage<Void> processPut(HttpMessage message) {
        putMessages.add(message);
        CompletableFuture<Void> result = new CompletableFuture<>();
        result.complete(null);
        return result;
    }

    public List<HttpMessage> getPostMessages() {
        return postMessages;
    }

    public List<HttpMessage> getPutMessages() {
        return putMessages;
    }
}
