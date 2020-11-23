package io.quarkus.reactivemessaging.http.sink.app;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.vertx.core.buffer.Buffer;

@ApplicationScoped
public class HttpEmitter {

    @Channel("my-http-sink")
    Emitter<Object> emitter;

    @Channel("http-sink-with-path-param")
    Emitter<Object> emitterWithPathParam;

    @Channel("retrying-http-sink")
    Emitter<Object> retryingEmitter;

    @Incoming("custom-http-source")
    @Outgoing("custom-http-sink")
    Message<String> passThroughWithCustomSerializer(HttpMessage<Buffer> message) {
        return new Message<String>() {
            @Override
            public String getPayload() {
                return message.getPayload().toString();
            }

            @Override
            public Supplier<CompletionStage<Void>> getAck() {
                return message::ack;
            }
        };
    }
    
    public <T> void emitMessageWithPathParam(Message<T> message) {
        emitterWithPathParam.send(message);
    }

    public CompletionStage<Void> retryingEmitObject(Object message) {
        return retryingEmitter.send(message);
    }

    public CompletionStage<Void> emitObject(Object message) {
        return emitter.send(message);
    }
}