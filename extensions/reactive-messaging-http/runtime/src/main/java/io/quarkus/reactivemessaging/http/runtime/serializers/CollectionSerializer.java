package io.quarkus.reactivemessaging.http.runtime.serializers;

import java.util.Collection;

import org.jboss.logging.Logger;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CollectionSerializer implements Serializer<Collection<?>> {
    private static final Logger log = Logger.getLogger(CollectionSerializer.class);

    @Override
    public boolean handles(Object payload) {
        return payload instanceof Collection;
    }

    @Override
    public Buffer serialize(Collection<?> payload) {
        log.info("serializing " + payload); // mstodo remove
        JsonArray array = new JsonArray();
        for (Object element : payload) {
            array.add(JsonObject.mapFrom(element));
        }

        return array.toBuffer();
    }

    @Override
    public int getPriority() {
        // first try other serializers, try this one if they don't match
        return DEFAULT_PRIORITY - 1;
    }
}
