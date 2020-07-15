package io.quarkus.it.rest.client.server;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import io.quarkus.it.rest.client.HeaderPassingClient;

@Path("/headerEcho")
public class HeaderEchoService {

    @Produces("text/plain")
    @GET
    public String getHeader(@HeaderParam(HeaderPassingClient.COMPUTED_HEADER) String header,
            @HeaderParam(HeaderPassingClient.STATIC_COMPUTED_HEADER) String staticHeader) {
        return header + "," + staticHeader;
    }
}
