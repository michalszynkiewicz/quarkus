package io.quarkus.reactivemessaging.http.runtime.serializers;

import org.jboss.logging.Logger;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;

public class JsonArraySerializer implements Serializer<JsonArray> {
    private static final Logger log = Logger.getLogger(JsonArraySerializer.class);

    @Override
    public boolean handles(Object payload) {
        return payload instanceof JsonArray;
    }

    @Override
    public Buffer serialize(JsonArray payload) {
        log.info("serializing " + payload); // mstodo remove
        return payload.toBuffer();
    }
}
