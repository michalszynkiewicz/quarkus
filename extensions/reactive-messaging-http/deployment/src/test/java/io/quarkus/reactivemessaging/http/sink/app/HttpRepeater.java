package io.quarkus.reactivemessaging.http.sink.app;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;

// mstodo no need for repeating, emitter is enough for tests
@ApplicationScoped
public class HttpRepeater {
    private static final Logger log = Logger.getLogger(HttpRepeater.class);

    public static final String HEADER_NAME = "http-header-from-repeater";
    public static final String HEADER_VALUE = "headerValue";

    // mstodo add a test for serialized json

    // mstodo try collection instead of JsonObject
    //    @Incoming("post-http-source")
    //    @Outgoing("my-http-sink")
    //    HttpMessage<Buffer> passThrough(HttpMessage<Buffer> message) {
    //        return message;
    //    }

    @Incoming("custom-http-source")
    @Outgoing("custom-http-sink")
    HttpMessage<String> passThroughWithCustomSerializer(HttpMessage<Buffer> message) {
        HttpMessage<String> newMessage = new HttpMessage<>(message.getPayload().toString(),
                MultiMap.caseInsensitiveMultiMap().add(HEADER_NAME, HEADER_VALUE));
        return newMessage;
    }

    public static class Dto {
        String field;

        public Dto(String value) {
            field = value;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }

    @Channel("my-http-sink")
    Emitter<Buffer> emitter;
    //    Emitter<HttpMessage<Collection<?>>> emitter;

    public CompletionStage<Void> emitMessage(HttpMessage<Collection<?>> message) {
        try {
            Collection<?> payload = message.getPayload();
            log.infof("emitting message %s", payload);
            Buffer buffer = Buffer.buffer(payload.toString());
            return emitter.send(buffer).whenComplete((success, failure) -> {
                if (failure != null) {
                    log.errorf(failure, "failed to emit message %s", message);
                } else {
                    log.info("successfully emitted");
                }
            });
        } catch (RuntimeException any) {
            any.printStackTrace();
            throw any;
        }
    }
}
