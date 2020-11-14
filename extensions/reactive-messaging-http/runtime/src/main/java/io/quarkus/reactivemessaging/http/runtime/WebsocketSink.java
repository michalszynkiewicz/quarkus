package io.quarkus.reactivemessaging.http.runtime;

import static java.util.Arrays.asList;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.quarkus.reactivemessaging.http.runtime.serializers.Serializer;
import io.quarkus.reactivemessaging.http.runtime.serializers.SerializerFactoryBase;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.AsyncResultUni;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;

// mstodo jitter etc
// mstodo keeping the connection open
class WebsocketSink {
    private static final String WSS = "wss";
    private static final List<String> supportedSchemes = asList("ws", WSS);

    private final URI uri;
    private final HttpClient httpClient;
    private final SubscriberBuilder<Message<?>, Void> subscriber;
    private final boolean ssl;
    private final String serializer;
    private final SerializerFactoryBase serializerFactory;

    WebsocketSink(Vertx vertx, URI uri, String serializer, SerializerFactoryBase serializerFactory) {
        this.uri = uri;
        this.serializerFactory = serializerFactory;
        this.serializer = serializer;

        String scheme = uri.getScheme().toLowerCase(Locale.getDefault());
        if (!supportedSchemes.contains(scheme)) {
            throw new IllegalArgumentException("Invalid scheme '" + scheme + "' for the websocket sink URL: " + uri);
        }
        ssl = WSS.equals(scheme);

        httpClient = vertx.createHttpClient();
        subscriber = ReactiveStreams.<Message<?>> builder()
                .flatMapCompletionStage(m -> send(m)
                        .onItem().transformToUni(v -> Uni.createFrom().completionStage(m.ack().thenApply(x -> m)))
                        .subscribeAsCompletionStage())
                .ignore();
    }

    private void connect(WebSocketConnectOptions options, Handler<AsyncResult<WebSocket>> handler) {
        httpClient.webSocket(options, connectResult -> {
            if (connectResult.succeeded()) {
                handler.handle(connectResult);
            } else {
                handler.handle(Future.failedFuture(connectResult.cause()));
            }
        });
    }

    private Uni<Void> send(Message<?> message) {
        // mstodo keep the connection open between "sends"
        // mstodo we have one connection per one message here, vastly nonoptimal probably
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setSsl(ssl)
                .setHost(uri.getHost())
                .setPort(uri.getPort())
                .setURI(uri.getPath());

        return AsyncResultUni.<WebSocket> toUni(handler -> connect(options, handler))
                .onItem().transformToUni(webSocket -> {
                    Serializer<Object> serializer = serializerFactory.getSerializer(this.serializer, message.getPayload());
                    Buffer serialized = serializer.serialize(message.getPayload());
                    return AsyncResultUni.<Void> toUni(handler -> _send(webSocket, serialized, handler));
                })
                .onFailure().invoke(message::nack)
                .onItem().invoke(message::ack);
    }

    private void _send(WebSocket webSocket, Buffer serialized, Handler<AsyncResult<Void>> handler) {
        webSocket.write(serialized, writeResult -> {
            if (writeResult.succeeded()) {
                handler.handle(Future.succeededFuture());
            } else {
                Throwable cause = writeResult.cause();
                handler.handle(Future.failedFuture(cause));
            }
        });
    }

    SubscriberBuilder<Message<?>, Void> sink() {
        return subscriber;
    }
}
