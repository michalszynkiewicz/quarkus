package io.quarkus.reactivemessaging.http.sink.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class HttpEndpoint {
    private List<Request> receivedRequests = new ArrayList<>();

    @Route(path = "/some-target-url", methods = HttpMethod.POST)
    void handlePost(RoutingContext ctx) {
        ctx.getBodyAsString();
        ctx.request().headers().entries();
        receivedRequests.add(new Request(ctx.getBodyAsString(), ctx.request().headers().entries()));
    }

    public List<Request> getReceivedRequests() {
        return receivedRequests;
    }

    public static class Request {
        String body;
        Map<String, List<String>> headers;

        public Request(String body, List<Map.Entry<String, String>> headers) {
            this.body = body;
            this.headers = new HashMap<>();
            for (Map.Entry<String, String> header : headers) {
                this.headers.computeIfAbsent(header.getKey(), whatever -> new ArrayList<>()).add(header.getValue());
            }
        }
    }
}
