package io.quarkus.reactivemessaging.http.runtime.serializers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SerializerFactoryBase {
    private final Map<String, Serializer> serializersByClassName = new HashMap<>();
    private final List<Serializer> predefinedSerializers = new ArrayList<>();

    SerializerFactoryBase() {
        // mstodo add more
        predefinedSerializers.add(new JsonObjectSerializer());
        predefinedSerializers.add(new JsonArraySerializer());
        predefinedSerializers.add(new StringSerializer());
        predefinedSerializers.add(new BufferSerializer());
        initAdditionalSerializers();
    }

    protected abstract void initAdditionalSerializers();

    public <T> Serializer<T> getSerializer(String name, T payload) {
        if (name != null) {
            Serializer serializer = serializersByClassName.get(name);
            if (serializer == null) {
                // mstodo fail miserably
                throw new IllegalArgumentException("No serializer class found for name: " + name);
            }
            return serializer;
        } else {
            if (payload == null) {
                // mstodo fail
                throw new IllegalArgumentException("Payload cannot be null");
            }
            for (Serializer serializer : predefinedSerializers) {
                if (serializer.handles(payload)) {
                    return serializer;
                }
            }
            // mstodo fail
            throw new IllegalArgumentException("No predefined serializer found matching class: " + payload.getClass());
        }
    }

    void addSerializer(String className, Serializer serializer) {
        serializersByClassName.put(className, serializer);
    }

}
