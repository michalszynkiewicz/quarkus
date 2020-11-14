package io.quarkus.reactivemessaging.http.sink.app;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.vertx.core.buffer.Buffer;

// mstodo no need for repeating, emitter is enough for tests
@ApplicationScoped
public class HttpEmitter {
    private static final Logger log = Logger.getLogger(HttpEmitter.class);

    public static final String HEADER_NAME = "http-header-from-repeater";
    public static final String HEADER_VALUE = "headerValue";

    @Incoming("custom-http-source")
    @Outgoing("custom-http-sink")
    Message<String> passThroughWithCustomSerializer(HttpMessage<Buffer> message) {
        return new Message<String>() {
            @Override
            public String getPayload() {
                return message.toString();
            }
        };
    }

    @Channel("my-http-sink")
    Emitter<Object> emitter;

    @Channel("retrying-http-sink")
    Emitter<Object> retryingEmitter;

    public CompletionStage<Void> retryingEmitMessage(Object message) {
        return retryingEmitter.send(message);
    }

    public CompletionStage<Void> emitMessage(Object message) {
        return emitter.send(message);
    }
}
