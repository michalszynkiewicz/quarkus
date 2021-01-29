package io.quarkus.rest.rest.client.microprofile;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

@Priority(Integer.MAX_VALUE)
public class MicroProfileRestClientResponseFilter implements ClientResponseFilter {

    private final AsyncHandlerProvider.Handler handler;

    public MicroProfileRestClientResponseFilter(AsyncHandlerProvider.Handler handler) {
        this.handler = handler;
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        handler.stop();
    }
}
