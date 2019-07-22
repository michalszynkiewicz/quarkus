package io.quarkus.reactivemessaging.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 22/07/2019
 */
public interface Deserializer<T> {
    Deserializer<byte[]> BYTE_ARRAY = new Deserializer<byte[]>() {
        @Override
        public byte[] deserialize(HttpServletRequest request) throws IOException {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            try (InputStream input = request.getInputStream()) {
                while ((read = input.read(buffer)) > 0) {
                    result.write(buffer, 0, read);
                }
            }
            return result.toByteArray();
        }
    };
    Deserializer<JsonObject> JSON = new Deserializer<JsonObject>() {

        @Override
        public JsonObject deserialize(HttpServletRequest request) throws Exception {
            try (JsonReader jsonReader = Json.createReader(request.getReader())) {
                return jsonReader.readObject();
            }
        }
    };
    Deserializer<String> STRING = new Deserializer<String>() {
        @Override
        public String deserialize(HttpServletRequest request) throws Exception {
            return request.getReader()
                    .lines()
                    .collect(Collectors.joining("\n"));
        }
    };

    T deserialize(HttpServletRequest request) throws Exception;
}
