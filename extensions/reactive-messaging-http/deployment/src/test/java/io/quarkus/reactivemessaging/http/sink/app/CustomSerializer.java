package io.quarkus.reactivemessaging.http.sink.app;

import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.serializers.Serializer;
import io.vertx.core.buffer.Buffer;

public class CustomSerializer implements Serializer<String> {
    private static final Logger log = Logger.getLogger(CustomSerializer.class);

    // mstodo priorities?
    @Override
    public boolean handles(Object payload) {
        log.infof("checking if %s of type %s is handled", payload, payload.getClass());
        return payload instanceof String;
    }

    @Override
    public Buffer serialize(String payload) {
        log.infof("serializing %s", payload);
        return Buffer.buffer(payload.toUpperCase());
    }
}
