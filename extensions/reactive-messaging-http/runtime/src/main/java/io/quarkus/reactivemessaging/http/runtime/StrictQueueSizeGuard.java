package io.quarkus.reactivemessaging.http.runtime;

import java.util.concurrent.atomic.AtomicInteger;

// mstodo try replacing it with checking the `requested` value
class StrictQueueSizeGuard {
    private final int queueSize;
    private final AtomicInteger enqueued = new AtomicInteger();

    StrictQueueSizeGuard(int queueSize) {
        this.queueSize = queueSize;
    }

    // mstodo this should be done before the message processing is started!!!
    // mstodo otherwise we only allow queueSize messages to be processed, not very reactive ;)
    void dequeue() {
        enqueued.decrementAndGet();
    }

    boolean prepareToEmit() {
        while (true) {
            int oldVal = enqueued.get();
            int newVal = oldVal + 1;
            if (newVal <= queueSize) {
                if (enqueued.compareAndSet(oldVal, newVal)) {
                    return true;
                } // else try again
            } else {
                return false; // too many messages to enqueue
            }
        }
    }
}
