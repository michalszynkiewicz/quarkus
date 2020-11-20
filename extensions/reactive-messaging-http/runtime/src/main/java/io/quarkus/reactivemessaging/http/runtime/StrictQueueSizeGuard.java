package io.quarkus.reactivemessaging.http.runtime;

import java.util.concurrent.atomic.AtomicInteger;

class StrictQueueSizeGuard {
    private final int queueSize;
    private final AtomicInteger enqueued = new AtomicInteger();

    StrictQueueSizeGuard(int queueSize) {
        this.queueSize = queueSize;
    }

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
