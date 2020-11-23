package io.quarkus.reactivemessaging.http.runtime.serializers;

import io.vertx.core.buffer.Buffer;

public interface Serializer<PayloadType> {
    int DEFAULT_PRIORITY = 0;

    boolean handles(Object payload);

    Buffer serialize(PayloadType payload);

    default int getPriority() {
        return DEFAULT_PRIORITY;
    }
}
