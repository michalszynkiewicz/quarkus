package io.quarkus.reactivemessaging.http.runtime.converters;

import java.lang.reflect.Type;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.MessageConverter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;

@ApplicationScoped
public class JsonArrayConverter implements MessageConverter {

    @Override
    public boolean canConvert(Message<?> in, Type target) {
        return in.getPayload() instanceof Buffer && target == JsonArray.class;
    }

    @Override
    public Message<JsonArray> convert(Message<?> in, Type target) {
        return in.withPayload(new JsonArray((Buffer) in.getPayload()));
    }
}
