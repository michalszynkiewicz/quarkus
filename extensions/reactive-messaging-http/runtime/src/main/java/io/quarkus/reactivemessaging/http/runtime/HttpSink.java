package io.quarkus.reactivemessaging.http.runtime;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class HttpSink {

    private final SubscriberBuilder<HttpMessage, Void> subscriber;
    private final WebClient client;
    private final String method;
    private final String url;

    public HttpSink(Vertx vertx, String method, String url) {
        //  mstodo drop or use       HttpClient client = vertx.createHttpClient();
        // mstodo: what's the difference between webclient and http client?

        client = WebClient.create(io.vertx.mutiny.core.Vertx.newInstance(vertx));
        subscriber = ReactiveStreams.<HttpMessage> builder()
                .flatMapCompletionStage(m -> send(m)
                        .onItem().transformToUni(v -> Uni.createFrom().completionStage(m.ack().thenApply(x -> m)))
                        .onSubscribe().invoke(() -> System.out.println("someone subscribed"))
                        .subscribeAsCompletionStage())
                .ignore();
        this.method = method;// mstodo
        this.url = url;// mstodo
    }

    public SubscriberBuilder<HttpMessage, Void> sink() {
        return subscriber;
    }

    private Uni<Void> send(HttpMessage message) {
        //        Serializer<Object> serializer = Serializer.lookup(message.getPayload(), converterClass);
        HttpRequest<?> request = toHttpRequest(message);
        return invoke(request, Buffer.buffer(message.getPayload().getByteBuf())) // mstodo
                .onItem().transformToUni(x -> Uni.createFrom().completionStage(message.ack()));
    }

    private Uni<Void> invoke(HttpRequest<?> request, Buffer buffer) {
        System.out.println("invoke"); // mstodo drop it
        Uni<? extends HttpResponse<?>> response = request
                .sendBuffer(buffer);
        response.onFailure().call(error -> {
            System.out.println("eror caught");
            error.printStackTrace();
            return Uni.createFrom().item("foo");
        });
        return response
                .map(whatever -> {
                    System.out.println("peeking :O");
                    return whatever;
                })
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
