package io.quarkus.resteasy.reactive.client.runtime;

import java.lang.reflect.Field;

@SuppressWarnings("unused")
// mstodo check if used ;)
public class ReflectionUtil {
    // mstodo try to get rid of it?
    public static Object readField(Object object, Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return field.get(object);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalArgumentException("Cannot read '" + fieldName + "' field from " + object + " of class " + clazz);
        }
    }
}
