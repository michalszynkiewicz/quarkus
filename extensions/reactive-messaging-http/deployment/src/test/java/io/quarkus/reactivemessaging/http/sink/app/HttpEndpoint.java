package io.quarkus.reactivemessaging.http.sink.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@ApplicationScoped
@Path("/recorder")
// mstodo change to JAX-RS, reactive routes may get dropped soon
public class HttpEndpoint {
    private List<Request> receivedRequests = new ArrayList<>();
    private AtomicInteger initialFailures = new AtomicInteger(0);
    private ReadWriteLock consumptionLock = new ReentrantReadWriteLock();

    @POST
    public Response handlePost(String body) {
        System.out.println("in handle post for " + body);
        consumptionLock.readLock().lock();
        try {
            if (initialFailures.getAndDecrement() > 0) {
                return Response.status(500).entity("forced failure").build();
            }
            receivedRequests.add(new Request(body));
            return Response.ok().entity("bye").build();
        } finally {
            consumptionLock.readLock().unlock();
        }
    }

    public List<Request> getReceivedRequests() {
        return receivedRequests;
    }

    public static class Request {
        String body;

        public Request(String body) {
            this.body = body;
        }

        public String getBody() {
            return body;
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
