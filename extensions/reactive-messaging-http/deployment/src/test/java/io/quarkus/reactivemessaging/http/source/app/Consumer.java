package io.quarkus.reactivemessaging.http.source.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.vertx.core.Vertx;
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

    private final List<Long> timers = Collections.synchronizedList(new ArrayList<>());

    @Inject
    Vertx vertx;

    volatile boolean ready = true;

    @Incoming("post-http-source")
    public CompletionStage<Void> process(HttpMessage<?> message) throws InterruptedException {
        CompletableFuture<Void> result = new CompletableFuture<>();

        triggerWhenReady(() -> {
            result.complete(null);
            postMessages.add(message);
            message.ack();
        }, System.currentTimeMillis(), 10000);
        return result;
    }

    private void triggerWhenReady(Runnable action, long startTime, long maxTime) {
        if (System.currentTimeMillis() - startTime >= maxTime) {
            throw new RuntimeException("the consumer not released in " + maxTime + " ms");
        }
        if (!ready) {
            timers.add(vertx.setTimer(100, timer -> triggerWhenReady(action, startTime, maxTime)));
        } else {
            action.run();
        }
    }

    @Incoming("put-http-source")
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
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
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
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

    public void pause() {
        ready = false;
    }

    public void resume() {
        ready = true;
    }

    public void clear() {
        postMessages.clear();
        putMessages.clear();
        payloads.clear();
        for (Long timer : timers) {
            vertx.cancelTimer(timer);
        }
        ready = true;
    }
}
