package io.quarkus.reactivemessaging.http.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;

/**
 * used by http source
 * 
 * @param <T> payload type
 */
public class HttpMessage<T> implements Message<T> {

    private static final Logger log = Logger.getLogger(HttpMessage.class);

    private final T payload;
    private final MultiMap httpHeaders;
    private final HttpServerResponse response;
    private final Runnable finalizer;

    public HttpMessage(T payload, MultiMap httpHeaders, HttpServerResponse response, Runnable finalizer) {
        this.payload = payload;
        this.httpHeaders = httpHeaders;
        this.response = response;
        this.finalizer = finalizer;
    }

    @Override
    public T getPayload() {
        return payload;
    }

    public MultiMap getHttpHeaders() {
        return httpHeaders;
    }

    @Override
    public CompletionStage<Void> ack() {
        System.out.println("in the ack");
        return getAck().get();
    }

    @Override
    public Supplier<CompletionStage<Void>> getAck() {
        return () -> CompletableFuture.runAsync(() -> {
            System.out.println("success, will run finalizer!"); // mstodo remove
            finalizer.run();
            System.out.println("finalizer executed, responding!"); // mstodo remove
            try {
                //                response.setStatusCode(202).end(); // mstodo can it be here?
            } catch (Throwable any) {
                System.out.println("failed to send back the status code");
                any.printStackTrace();
            }
            System.out.println("response sent!"); // mstodo remove
        });
    }

    @Override
    public Function<Throwable, CompletionStage<Void>> getNack() {
        return error -> CompletableFuture.runAsync(() -> {
            finalizer.run();
            if (error instanceof ReactiveHttpException) {
                response.setStatusCode(((ReactiveHttpException) error).getStatusCode()).end(error.getMessage()); // mstodo proper handling
            } else {
                response.setStatusCode(500).end("Failed to process ");
                log.error("Failed to process messsage.", error);
            }
        });
    }
}
