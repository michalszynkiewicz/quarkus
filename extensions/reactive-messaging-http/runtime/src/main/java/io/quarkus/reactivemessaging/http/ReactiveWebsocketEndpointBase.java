package io.quarkus.reactivemessaging.http;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.websocket.OnMessage;

import org.reactivestreams.Processor;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 15/07/2019
 */
@ApplicationScoped
public abstract class ReactiveWebsocketEndpointBase {

    private Processor<WebsocketMessage<?>, WebsocketMessage<?>> processor;

    public void initializeProcessor(@Observes WebsocketProcessorCreated event) {
        System.out.println("got event: " + event); // mstodo remove
        if (event.matchesPath(path())) {
            processor = event.getProcessor();
        } else {
            System.out.println("not matching"); // mstodo
        }
    }

    // mstodo: on parsing error call processor.onError?
    @OnMessage
    public void onMessage(String message) {
        System.out.println("got message " + message + " for path: " + path());
        // mstodo check for processor == null
        processor.onNext(new WebsocketMessage<>(message));
    }

    // mstodo: on parsing error call processor.onError?
    @OnMessage
    public void onMessage(byte[] message) {
        processor.onNext(new WebsocketMessage<>(message));
    }

    protected abstract String path();
}
