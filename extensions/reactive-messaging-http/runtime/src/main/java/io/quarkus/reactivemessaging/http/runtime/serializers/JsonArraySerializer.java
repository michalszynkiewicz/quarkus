package io.quarkus.reactivemessaging.http.runtime.serializers;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;

public class JsonArraySerializer implements Serializer<JsonArray> {
    @Override
    public boolean handles(Object payload) {
        return payload instanceof JsonArray;
    }

    @Override
    public Buffer serialize(JsonArray payload) {
        return payload.toBuffer();
    }
}
