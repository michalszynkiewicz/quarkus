package io.quarkus.reactivemessaging.http.runtime.serializers;

import io.vertx.core.buffer.Buffer;

public interface Serializer<PayloadType> {
    boolean handles(Object payload);

    // mstodo should probably be Uni
    Buffer serialize(PayloadType payload);
}
