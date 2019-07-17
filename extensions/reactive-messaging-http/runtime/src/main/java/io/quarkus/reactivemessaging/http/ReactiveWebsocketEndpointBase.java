package io.quarkus.reactivemessaging.http;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnMessage;

import org.reactivestreams.Processor;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 15/07/2019
 */
@ApplicationScoped
public abstract class ReactiveWebsocketEndpointBase {

    // mstodo: on parsing error call processor.onError?
    @OnMessage
    public void onMessage(String message) {
        System.out.println("got message " + message + " for path: " + path());
        Processor<WebsocketMessage<?>, WebsocketMessage<?>> processor = getProcessor();
        processor.onNext(new WebsocketMessage<>(message));
    }

    // mstodo: on parsing error call processor.onError?
    @OnMessage
    public void onMessage(byte[] message) {
        Processor<WebsocketMessage<?>, WebsocketMessage<?>> processor = getProcessor();
        processor.onNext(new WebsocketMessage<>(message));
    }

    private Processor<WebsocketMessage<?>, WebsocketMessage<?>> getProcessor() {
        return connector().getProcessor(path());
    }

    protected abstract String path();

    protected abstract QuarkusWebsocketConnector connector();
}
