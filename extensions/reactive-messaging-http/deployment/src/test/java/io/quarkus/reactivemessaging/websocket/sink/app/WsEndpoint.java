package io.quarkus.reactivemessaging.websocket.sink.app;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

import org.jboss.logging.Logger;

@ApplicationScoped
@ServerEndpoint("/ws-target-url")
public class WsEndpoint {
    private static final Logger log = Logger.getLogger(WsEndpoint.class);
    private final List<String> messages = new ArrayList<>();

    @OnError
    void onError(Throwable error) {
        log.error("Unexpected error in the test websocket server", error);
    }

    @OnMessage
    void consumeMessage(byte[] message) {
        System.out.println("onMessage::binary");
        messages.add(new String(message));
    }

    public List<String> getMessages() {
        return messages;
    }
}
