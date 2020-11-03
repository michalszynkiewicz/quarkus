package io.quarkus.reactivemessaging.http.runtime.converters;

import java.lang.reflect.Type;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.MessageConverter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class JsonObjectConverter implements MessageConverter {

    @Override
    public boolean canConvert(Message<?> in, Type target) {
        return in.getPayload() instanceof Buffer && target == JsonObject.class;
    }

    @Override
    public Message<JsonObject> convert(Message<?> in, Type target) {
        return in.withPayload(new JsonObject((Buffer) in.getPayload()));
    }
}
