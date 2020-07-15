package io.quarkus.it.rest.client;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/header")
public class HeaderClientResource {

    @RestClient
    @Inject
    HeaderPassingClient client;

    @Produces("text/plain")
    @GET
    public String getPassedHeader() {
        return client.getHeader();
    }
}
