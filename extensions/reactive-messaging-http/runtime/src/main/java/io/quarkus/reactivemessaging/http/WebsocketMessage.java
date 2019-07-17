package io.quarkus.reactivemessaging.http;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * mstodo: for now only text is supported
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 15/07/2019
 */
public class WebsocketMessage<T> implements Message<T> {

    private final String textContent;
    private final byte[] binaryContent;

    public WebsocketMessage(String content) {
        textContent = content;
        binaryContent = null;
    }

    public WebsocketMessage(byte[] content) {
        binaryContent = content;
        textContent = null;
    }

    @Override
    public T getPayload() {
        return (T) (textContent == null ? binaryContent : textContent); // mstodo - it doesn't make sense :D
    }

    @Override
    public CompletionStage<Void> ack() {
        return null;
    }
}
