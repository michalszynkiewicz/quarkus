package io.quarkus.reactivemessaging.http.runtime;

import static java.util.Arrays.asList;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocketConnectOptions;

public class WebsocketSink {
    public static final String WSS = "wss";
    private static final List<String> supportedSchemes = asList("ws", WSS);

    private final URI uri;
    private final HttpClient httpClient;
    private final SubscriberBuilder<WebsocketMessage, Void> subscriber;
    private final boolean ssl;

    public WebsocketSink(Vertx vertx, URI uri) {
        httpClient = vertx.createHttpClient();
        subscriber = ReactiveStreams.<WebsocketMessage> builder()
                .flatMapCompletionStage(m -> send(m)
                        .onItem().transformToUni(v -> Uni.createFrom().completionStage(m.ack().thenApply(x -> m)))
                        .onSubscribe().invoke(() -> System.out.println("someone subscribed")) // mstodo remove
                        .subscribeAsCompletionStage())
                .ignore();
        this.uri = uri;// mstodo

        String scheme = uri.getScheme().toLowerCase(Locale.getDefault());
        if (!supportedSchemes.contains(scheme)) {
            throw new IllegalStateException("Invalid scheme '" + scheme + "' for the websocket sink URL: " + uri); // mstodo is this the proper exception
        }
        ssl = WSS.equals(scheme);
    }

    private Uni<Object> send(WebsocketMessage message) {
        // mstodo keep the connection open between "sends"
        CompletableFuture<Object> result = new CompletableFuture<>();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setSsl(ssl)
                .setHost(uri.getHost())
                .setPort(uri.getPort())
                .setURI(uri.getPath()); // mstodo
        httpClient.webSocket(options, connectResult -> {
            if (connectResult.succeeded()) {
                // mstodo differentiate  between text and binary messages, etc
                connectResult.result().write(message.getPayload(), writeResult -> {
                    System.out.println("write done");
                    if (writeResult.succeeded()) {
                        System.out.println("write succeeded");
                        message.ack().thenAccept(whatever -> result.complete(null));
                    } else {
                        Throwable cause = writeResult.cause();
                        System.out.println("write failed");
                        cause.printStackTrace(); // mstodo drop printlns and stacktrace
                        // mstodo this and the above - the good path - don't look good
                        message.nack(cause).thenAccept(whatever -> result.completeExceptionally(cause));
                    }
                });
            } else {
                // mstodo do better
                message.nack(connectResult.cause()).thenAccept(ignored -> result.completeExceptionally(connectResult.cause()));
            }
        });
        return Uni.createFrom().completionStage(result);
    }

    public SubscriberBuilder<WebsocketMessage, Void> sink() {
        System.out.println("returned subscriber"); // mstodo remove
        return subscriber;
    }
}
