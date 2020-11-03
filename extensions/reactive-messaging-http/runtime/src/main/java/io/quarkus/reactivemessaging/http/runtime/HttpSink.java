package io.quarkus.reactivemessaging.http.runtime;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.quarkus.reactivemessaging.http.runtime.serializers.Serializer;
import io.quarkus.reactivemessaging.http.runtime.serializers.SerializerFactoryBase;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class HttpSink {

    private final SubscriberBuilder<HttpMessage<?>, Void> subscriber;
    private final WebClient client;
    private final String method;
    private final String url;
    private final SerializerFactoryBase serializerFactory;
    private final String serializerName;

    HttpSink(Vertx vertx, String method, String url,
            String serializerName,
            SerializerFactoryBase serializerFactory) {
        //  mstodo drop or use       HttpClient client = vertx.createHttpClient();
        // mstodo: what's the difference between webclient and http client?

        client = WebClient.create(io.vertx.mutiny.core.Vertx.newInstance(vertx));
        subscriber = ReactiveStreams.<HttpMessage<?>> builder()
                .flatMapCompletionStage(m -> send(m)
                        .onItem().transformToUni(v -> Uni.createFrom().completionStage(m.ack().thenApply(x -> m)))
                        .subscribeAsCompletionStage())
                .ignore();
        this.method = method;// mstodo
        this.url = url;// mstodo
        this.serializerFactory = serializerFactory;

        this.serializerName = serializerName;
    }

    public SubscriberBuilder<HttpMessage<?>, Void> sink() {
        return subscriber;
    }

    private Uni<Void> send(HttpMessage<?> message) {
        HttpRequest<?> request = toHttpRequest(message);
        Buffer payload = serialize(message.getPayload()); // mstodo cache serializer!?
        return invoke(request, payload) // mstodo
                .onItem().transformToUni(x -> Uni.createFrom().completionStage(message.ack()));
    }

    // mstodo clean-up generics
    private Buffer serialize(Object payload) {
        System.out.println("will select a serializer and serialize " + payload); // mstodo drop it

        io.vertx.core.buffer.Buffer buffer; // mstodo clean up the try
        try {
            Serializer serializer = serializerFactory.getSerializer(serializerName, payload); // mstodo proper error handling for this!!!
            buffer = serializer.serialize(payload);
        } catch (Exception any) {
            any.printStackTrace();
            throw new RuntimeException(any);
        }
        System.out.println("serialized " + buffer.toString()); // mstodo drop it

        // mstodo maybe the serializers could produce both kinds of buffers?
        return Buffer.buffer(buffer.getBytes());
    }

    private Uni<Void> invoke(HttpRequest<?> request, Buffer buffer) {
        Uni<? extends HttpResponse<?>> response = request
                .sendBuffer(buffer);
        System.out.println("will send " + buffer.toString()); // mstodo drop it
        response.onFailure().call(error -> {
            System.out.println("error caught"); // mstodo error handling
            error.printStackTrace();
            return Uni.createFrom().item("foo");
        });
        return response
                .onItem().transform(resp -> {
                    // mstodo delete souts
                    System.out.println("got some response, status code: " + resp.statusCode());
                    System.out.println("response body: " + resp.bodyAsString("UTF-8"));
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        return null;
                    } else {
                        throw new RuntimeException("Invalid status code"); // mstodo
                    }
                });
    }

    private HttpRequest<?> toHttpRequest(HttpMessage message) {
        //        HttpResponseMetadata metadata = message.getMetadata(HttpResponseMetadata.class).orElse(null);
        //        String actualUrl = metadata != null && metadata.getUrl() != null ? metadata.getUrl() : this.url;
        //        String actualMethod = metadata != null && metadata.getMethod() != null ? metadata.getMethod().toUpperCase()
        //                : this.method.toUpperCase();
        //        Map<String, ?> httpHeaders = metadata != null ? metadata.getHeaders() : Collections.emptyMap();
        //        Map<String, ?> query = metadata != null ? metadata.getQuery() : Collections.emptyMap();

        HttpRequest<Buffer> request;
        switch (method) {
            case "POST":
                request = client.postAbs(url);
                break;
            case "PUT":
                request = client.putAbs(url);
                break;
            default:
                throw new IllegalArgumentException("Unsupported ");
        }

        //        MultiMap requestHttpHeaders = request.headers();
        //        httpHeaders.forEach((k, v) -> {
        //            if (v instanceof Collection) {
        //                ((Collection<Object>) v).forEach(item -> requestHttpHeaders.add(k, item.toString()));
        //            } else {
        //                requestHttpHeaders.add(k, v.toString());
        //            }
        //        });
        //        query.forEach((k, v) -> {
        //            if (v instanceof Collection) {
        //                ((Collection<Object>) v).forEach(item -> request.addQueryParam(k, item.toString()));
        //            } else {
        //                request.addQueryParam(k, v.toString());
        //            }
        //        });

        return request;
    }
}
