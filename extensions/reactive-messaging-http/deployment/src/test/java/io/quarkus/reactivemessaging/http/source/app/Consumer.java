package io.quarkus.reactivemessaging.http.source.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class Consumer {
    private static final Logger log = Logger.getLogger(Consumer.class);

    private static final CompletableFuture<Void> COMPLETED;

    static {
        COMPLETED = new CompletableFuture<>();
        COMPLETED.complete(null);
    }

    private final List<HttpMessage<?>> postMessages = new ArrayList<>();
    private final List<HttpMessage<?>> putMessages = new ArrayList<>();
    private final List<Object> payloads = new ArrayList<>();

    private Semaphore semaphore = new Semaphore(1000);

    // mstodo why do we need acknowledgements here suddenly?
    @Incoming("post-http-source")
    //    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public CompletionStage<Void> process(HttpMessage<?> message) throws InterruptedException {
        log.info("--waiting for processing of " + message.getPayload().toString());
        semaphore.acquire();
        log.info("--processed " + message.getPayload().toString());
        postMessages.add(message);
        message.ack();
        return COMPLETED;
        //        return COMPLETED;
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
        semaphore.drainPermits();
    }

    public void resume() {
        semaphore.release(1000);
        System.out.println("Semaphore released");
        System.out.flush();
    }

    public void clear() {
        postMessages.clear();
        putMessages.clear();
        payloads.clear();
        resume();
    }
}
