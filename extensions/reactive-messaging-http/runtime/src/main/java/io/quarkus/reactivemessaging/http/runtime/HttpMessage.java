package io.quarkus.reactivemessaging.http.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import io.vertx.core.MultiMap;

/**
 * used by http source
 * 
 * @param <T> payload type
 */
public class HttpMessage<T> implements Message<T> {

    private static final Logger log = Logger.getLogger(HttpMessage.class);

    private final T payload;
    private final MultiMap httpHeaders;
    private final Runnable successHandler;
    private final Consumer<Throwable> failureHandler;

    public HttpMessage(T payload, MultiMap httpHeaders, Runnable successHandler, Consumer<Throwable> failureHandler) {
        this.payload = payload;
        this.httpHeaders = httpHeaders;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Override
    public T getPayload() {
        return payload;
    }

    // mstodo remove?
    public MultiMap getHttpHeaders() {
        return httpHeaders;
    }

    @Override
    public Supplier<CompletionStage<Void>> getAck() {
        return () -> CompletableFuture.runAsync(successHandler::run);
    }

    @Override
    public Function<Throwable, CompletionStage<Void>> getNack() {
        return error -> CompletableFuture.runAsync(() -> {
            failureHandler.accept(error);
        });
    }
}
