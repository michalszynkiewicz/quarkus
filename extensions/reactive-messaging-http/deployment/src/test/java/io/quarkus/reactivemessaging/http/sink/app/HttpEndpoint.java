package io.quarkus.reactivemessaging.http.sink.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
// mstodo change to JAX-RS, reactive routes may get dropped soon
public class HttpEndpoint {
    private List<Request> receivedRequests = new ArrayList<>();
    private AtomicInteger initialFailures = new AtomicInteger(0);
    private ReadWriteLock consumptionLock = new ReentrantReadWriteLock();
    // mstodo: try to get rid of guard

    @Route(path = "/recorder", methods = HttpMethod.POST)
    void handlePost(RoutingContext ctx) throws InterruptedException {
        System.out.println("got an http message to consume");
        consumptionLock.readLock().lock();
        try {
            System.out.println("in da lock"); // mstodo remove printlns from here and emitter with backpressure
            if (initialFailures.getAndDecrement() > 0) {
                ctx.response().setStatusCode(500).end("forced failure");
                return;
            }
            receivedRequests.add(new Request(ctx.getBodyAsString(), ctx.request().headers().entries()));
            ctx.response().setStatusCode(200).end("bye");
        } finally {
            System.out.println("releasing the lock"); // mstodo remove printlns from here and emitter with backpressure
            consumptionLock.readLock().unlock();
        }
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

        public String getBody() {
            return body;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }
    }

    public void setInitialFailures(int initialFailures) {
        this.initialFailures.set(initialFailures);
    }

    public void reset() {
        receivedRequests.clear();
        initialFailures.set(0);
        try {
            consumptionLock.writeLock().unlock();
        } catch (RuntimeException ignored) {
        }
        try {
            consumptionLock.readLock().unlock();
        } catch (RuntimeException ignored) {
        }
    }

    @SuppressWarnings("LockAcquiredButNotSafelyReleased")
    public void pause() {
        consumptionLock.writeLock().lock();
    }

    public void release() {
        consumptionLock.writeLock().unlock();
    }
}
