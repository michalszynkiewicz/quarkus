package io.quarkus.reactivemessaging.http.runtime.serializers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

public abstract class SerializerFactoryBase {
    private static final Logger log = Logger.getLogger(SerializerFactoryBase.class);

    private final Map<String, Serializer> serializersByClassName = new HashMap<>();
    private final List<Serializer> predefinedSerializers = new ArrayList<>();

    SerializerFactoryBase() {
        // mstodo add more
        predefinedSerializers.add(new JsonObjectSerializer());
        predefinedSerializers.add(new JsonArraySerializer());
        predefinedSerializers.add(new StringSerializer());
        predefinedSerializers.add(new BufferSerializer());
        predefinedSerializers.add(new ObjectSerializer());
        predefinedSerializers.add(new CollectionSerializer());

        predefinedSerializers.sort(Comparator.comparingInt(Serializer::getPriority));
        Collections.reverse(predefinedSerializers);

        initAdditionalSerializers();
    }

    protected abstract void initAdditionalSerializers();

    public <T> Serializer<T> getSerializer(String name, T payload) {
        System.out.println("getting serializer"); // mstodo remove
        if (payload == null) {
            System.out.println("null payload"); // mstodo remove printlns
            // mstodo fail
            throw new IllegalArgumentException("Payload cannot be null");
        }
        if (name != null) {
            System.out.println("getting serializer for name: " + name); // mstodo remove
            @SuppressWarnings("unchecked")
            Serializer<T> serializer = serializersByClassName.get(name);
            if (serializer == null) {
                // mstodo fail miserably
                throw new IllegalArgumentException("No serializer class found for name: " + name);
            }
            if (serializer.handles(payload)) {
                return serializer;
            } else {
                log.warnf("Specified serializer (%s) does not handle the payload type %s", name, payload.getClass());
            }
        }
        for (Serializer<?> serializer : predefinedSerializers) {
            System.out.println("checking serializer: " + serializer.getClass()); // mstodo remove
            if (serializer.handles(payload)) {
                //noinspection unchecked
                return (Serializer<T>) serializer;
            }
        }
        // mstodo fail properly
        System.out.println("no serializer found");
        throw new IllegalArgumentException("No predefined serializer found matching class: " + payload.getClass());
    }

    @SuppressWarnings("unused") // used by a generated subclass
    void addSerializer(String className, Serializer serializer) {
        serializersByClassName.put(className, serializer);
    }

}
