package io.quarkus.reactivemessaging.http.runtime;

import static java.util.Arrays.asList;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.quarkus.reactivemessaging.http.runtime.serializers.Serializer;
import io.quarkus.reactivemessaging.http.runtime.serializers.SerializerFactoryBase;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocketConnectOptions;

public class WebsocketSink {
    private static final String WSS = "wss";
    private static final List<String> supportedSchemes = asList("ws", WSS);

    private final URI uri;
    private final HttpClient httpClient;
    private final SubscriberBuilder<Message<?>, Void> subscriber;
    private final boolean ssl;
    private final String serializer;
    private final SerializerFactoryBase serializerFactory;

    public WebsocketSink(Vertx vertx, URI uri, String serializer, SerializerFactoryBase serializerFactory) {
        httpClient = vertx.createHttpClient();
        subscriber = ReactiveStreams.<Message<?>> builder()
                .flatMapCompletionStage(m -> send(m)
                        .onItem().transformToUni(v -> Uni.createFrom().completionStage(m.ack().thenApply(x -> m)))
                        .subscribeAsCompletionStage())
                .ignore();
        this.uri = uri;
        this.serializerFactory = serializerFactory;
        this.serializer = serializer;

        String scheme = uri.getScheme().toLowerCase(Locale.getDefault());
        if (!supportedSchemes.contains(scheme)) {
            throw new IllegalStateException("Invalid scheme '" + scheme + "' for the websocket sink URL: " + uri); // mstodo is this the proper exception
        }
        ssl = WSS.equals(scheme);
    }

    private Uni<Object> send(Message<?> message) {
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
                // mstodo mvoe the logic to get serializer to serializer factory and use it here too
                Serializer<Object> serializer = serializerFactory.getSerializer(this.serializer, message.getPayload());
                Buffer serialized = serializer.serialize(message.getPayload()); // mstodo should be uni
                connectResult.result().write(serialized, writeResult -> {
                    if (writeResult.succeeded()) {
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

    public SubscriberBuilder<Message<?>, Void> sink() {
        return subscriber;
    }
}
