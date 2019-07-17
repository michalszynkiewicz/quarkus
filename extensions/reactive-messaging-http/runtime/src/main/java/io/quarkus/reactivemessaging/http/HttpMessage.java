package io.quarkus.reactivemessaging.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 09/07/2019
 */
public class HttpMessage<T> implements Message<T> {

    final T payload;

    /**
     * mstodo support deserializers
     */
    public HttpMessage(HttpServletRequest request) throws PayloadParsingException {
        String contentType = request.getContentType();
        //        if (type == byte[].class) {
        payload = (T) readByteArray(request);
        //        } else if (contentType != null && contentType.equalsIgnoreCase("application/json")) {
        //            payload = readJson(request, type);
        //        } else {
        //            throw new PayloadParsingException("Unable to parse payload to type: " + type + ". " +
        //                    "Non-json input can only be parsed to byte array");
        //        }
    }

    /*
     * mstodo: the following two ar enot used at the moment
     */
    private <T> T readJson(HttpServletRequest request, Class<T> type) throws PayloadParsingException {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.fromJson(request.getInputStream(), type);
        } catch (Exception e) {
            throw new PayloadParsingException("Failed to parse request to " + type.getName(), e);
        }
    }

    private byte[] readByteArray(HttpServletRequest request) throws PayloadParsingException {
        byte[] buffer = new byte[4096];
        int read;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            while ((read = request.getInputStream().read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new PayloadParsingException("Failed to read request content", e);
        }
    }

    @Override
    public T getPayload() {
        return payload;
    }
}
