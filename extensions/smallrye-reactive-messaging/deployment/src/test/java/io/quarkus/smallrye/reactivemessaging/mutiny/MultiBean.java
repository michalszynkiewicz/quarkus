package io.quarkus.smallrye.reactivemessaging.mutiny;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class MultiBean {
    private static final AtomicInteger count = new AtomicInteger(0);

    final List<Integer> results = Collections.synchronizedList(new ArrayList<>());

    public volatile Throwable failure;

    @Outgoing("number-producer")
    public Message<Integer> create() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Message.of(count.incrementAndGet());
    }

    @Incoming("number-producer")
    @Outgoing("even-numbers-producer")
    public Multi<Integer> timesTwo(Multi<Integer> input) {
        return input.map(i -> {
            int a = i * 2;
            System.out.println(a);
            return a;
        });
    }

    @Incoming("even-numbers-producer")
    public void collect(Integer input) {
        System.out.println("collected: " + input);
        results.add(input);
    }
}
