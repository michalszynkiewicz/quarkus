package io.quarkus.reactivemessaging.http.runtime;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.quarkus.reactivemessaging.http.runtime.serializers.Serializer;
import io.quarkus.reactivemessaging.http.runtime.serializers.SerializerFactoryBase;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniRetry;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

// mstodo test for headers
// mstodo test for query parameters
// TODO support path parameters?
class HttpSink {

    private final SubscriberBuilder<Message<?>, Void> subscriber;
    private final WebClient client;
    private final String method;
    private final String url;
    private final SerializerFactoryBase serializerFactory;
    private final String serializerName;

    HttpSink(Vertx vertx, String method, String url,
            String serializerName,
            int maxAttempts,
            double jitter,
            Optional<Duration> delay,
            SerializerFactoryBase serializerFactory) {
        this.method = method;
        this.url = url;
        this.serializerFactory = serializerFactory;
        this.serializerName = serializerName;

        client = WebClient.create(io.vertx.mutiny.core.Vertx.newInstance(vertx));
        subscriber = ReactiveStreams.<Message<?>> builder()
                .flatMapCompletionStage(m -> {
                    Uni<Void> send = send(m);
                    if (maxAttempts > 1) {
                        UniRetry<Void> retry = send // mstodo simplify the above if
                                .onFailure().retry();
                        if (delay.isPresent()) {
                            retry = retry.withBackOff(delay.get()).withJitter(jitter);
                        }
                        send = retry.atMost(maxAttempts);
                    }
                    return send
                            .onItemOrFailure().transformToUni((result, error) -> {
                                if (error != null) {
                                    return Uni.createFrom().completionStage(m.nack(error).thenApply(x -> m));
                                }
                                return Uni.createFrom().completionStage(m.ack().thenApply(x -> m));
                            })
                            .subscribeAsCompletionStage();
                }).ignore();
    }

    SubscriberBuilder<Message<?>, Void> sink() {
        return subscriber;
    }

    // mstodo non-blocking serialization?
    private Uni<Void> send(Message<?> message) {
        System.out.println("in send for " + message.getPayload()); // mstodo drop it
        HttpRequest<?> request = toHttpRequest(message);
        Buffer payload = serialize(message.getPayload());
        return Uni.createFrom().item(payload)
                .onItem().transformToUni(buffer -> invoke(request, buffer));
    }

    private <T> Buffer serialize(T payload) {
        Serializer<T> serializer = serializerFactory.getSerializer(serializerName, payload); // mstodo test error handling, like serializer throwing an error

        return Buffer.newInstance(serializer.serialize(payload));
    }

    private Uni<Void> invoke(HttpRequest<?> request, Buffer buffer) {
        return request
                .sendBuffer(buffer)
                .onItem().transform(resp -> {
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        return null;
                    } else {
                        System.out.println("wrong status code: " + resp.statusCode()); // mstodo remove
                        throw new RuntimeException("HTTP request returned an invalid status: " + resp.statusCode());
                    }
                });
    }

    private HttpRequest<?> toHttpRequest(Message<?> message) {
        HttpResponseMetadata metadata = message.getMetadata(HttpResponseMetadata.class).orElse((HttpResponseMetadata) null);

        MultiMap httpHeaders = metadata != null ? metadata.getHeaders() : MultiMap.caseInsensitiveMultiMap();
        Map<String, List<String>> query = metadata != null ? metadata.getQuery() : Collections.emptyMap();

        HttpRequest<Buffer> request;
        switch (method) {
            case "POST":
                request = client.postAbs(url);
                break;
            case "PUT":
                request = client.putAbs(url);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method + "only PUT and POST are supported");
        }

        request.putHeaders(new io.vertx.mutiny.core.MultiMap(httpHeaders));

        for (Map.Entry<String, List<String>> queryParam : query.entrySet()) {
            for (String queryParamValue : queryParam.getValue()) {
                request.addQueryParam(queryParam.getKey(), queryParamValue);
            }
        }

        return request;
    }
}
