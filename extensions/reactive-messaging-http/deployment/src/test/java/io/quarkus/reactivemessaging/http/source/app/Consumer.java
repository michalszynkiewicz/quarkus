package io.quarkus.reactivemessaging.http.source.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class Consumer {

    private static final CompletableFuture<Void> COMPLETED;

    static {
        COMPLETED = new CompletableFuture<>();
        COMPLETED.complete(null);
    }

    private final List<HttpMessage<?>> postMessages = new ArrayList<>();
    private final List<HttpMessage<?>> putMessages = new ArrayList<>();
    private final List<Object> payloads = new ArrayList<>();

    @Incoming("post-http-source")
    public CompletionStage<Void> process(HttpMessage<?> message) {
        postMessages.add(message);
        return COMPLETED;
    }

    @Incoming("put-http-source")
    public CompletionStage<Void> processPut(HttpMessage<?> message) {
        putMessages.add(message);
        return COMPLETED;
    }

    @Incoming("json-http-source")
    public CompletionStage<Void> processJsonObject(JsonObject jsonObject) {
        payloads.add(jsonObject);
        return COMPLETED;
    }

    @Incoming("jsonarray-http-source")
    public CompletionStage<Void> processJsonArray(JsonArray jsonArray) {
        payloads.add(jsonArray);
        return COMPLETED;
    }

    @Incoming("string-http-source")
    public CompletionStage<Void> processString(Message<String> stringMessage) {
        payloads.add(stringMessage.getPayload());
        return COMPLETED;
    }

    public List<HttpMessage<?>> getPostMessages() {
        return postMessages;
    }

    public List<HttpMessage<?>> getPutMessages() {
        return putMessages;
    }

    public List<?> getPayloads() {
        return payloads;
    }
}
