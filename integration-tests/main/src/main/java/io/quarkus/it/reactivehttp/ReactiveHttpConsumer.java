package io.quarkus.it.reactivehttp;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.reactivemessaging.http.HttpMessage;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 11/07/2019
 */
@Path("/reactive-http-endpoint")
public class ReactiveHttpConsumer {

    private List<String> receivedData = Collections.synchronizedList(new ArrayList<>());

    @Incoming("my-http-channel")
    public void consume(HttpMessage<?> message) throws UnsupportedEncodingException {
        String messageContent = new String((byte[]) message.getPayload(), "UTF-8");
        receivedData.add(messageContent);
    }

    @GET
    public String content() {
        return String.join(",", receivedData);
    }

    @DELETE
    public void clear() {
        receivedData.clear();
    }
}
