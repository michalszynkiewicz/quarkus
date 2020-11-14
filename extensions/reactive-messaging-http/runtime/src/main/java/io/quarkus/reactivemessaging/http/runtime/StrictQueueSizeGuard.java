package io.quarkus.reactivemessaging.http.runtime;

import java.util.concurrent.atomic.AtomicLong;

import org.jboss.logging.Logger;

class StrictQueueSizeGuard {
    private static final Logger log = Logger.getLogger(StrictQueueSizeGuard.class);

    private final int queueSize;
    private final AtomicLong enqueued = new AtomicLong(); // mstodo AtomicInteger?

    StrictQueueSizeGuard(int queueSize) {
        this.queueSize = queueSize;
    }

    void dequeue() {
        log.info("dequeued");
        long newSize = enqueued.decrementAndGet();
        log.info("newSize: " + newSize);
    }

    boolean prepareToEmit() {
        // mstodo remove p rintlns
        log.infof("checking if can be enqueued, queue size before: %d, max: %d", enqueued.get(), queueSize);
        while (true) {
            long oldVal = enqueued.get();
            long newVal = oldVal + 1;
            if (newVal <= queueSize) {
                log.info("can send");
                // mstodo can send
                if (enqueued.compareAndSet(oldVal, newVal)) {
                    log.info("can send indeed");
                    // mstodo can send
                    return true;
                } // else try again
            } else {
                log.info("cannot send");
                return false; // too many messages to enqueue
            }
        }
    }
}
