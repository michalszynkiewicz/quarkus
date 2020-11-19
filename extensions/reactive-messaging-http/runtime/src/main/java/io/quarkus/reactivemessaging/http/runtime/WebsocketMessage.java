package io.quarkus.reactivemessaging.http.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.reactive.messaging.Message;

public class WebsocketMessage<PayloadType> implements Message<PayloadType> {

    private final PayloadType payload;
    private final Runnable successHandler;
    private final Consumer<Throwable> failureHandler;

    public WebsocketMessage(PayloadType payload, Runnable successHandler, Consumer<Throwable> failureHandler) {
        this.payload = payload;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Override
    public PayloadType getPayload() {
        return payload;
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
