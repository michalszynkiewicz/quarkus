package io.quarkus.it.rest.client;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParams;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/headerEcho")
public interface HeaderPassingClient {

    String COMPUTED_VALUE = "someComputedValue";
    String COMPUTED_HEADER = "computedHeader";
    String STATIC_COMPUTED_HEADER = "staticComputedHeader";
    String STATIC_COMPUTED_VALUE = "staticComputedValue";

    @Produces("text/plain")
    @GET
    @ClientHeaderParams({ @ClientHeaderParam(name = COMPUTED_HEADER, value = "{computeTheHeader}"),
            @ClientHeaderParam(name = STATIC_COMPUTED_HEADER, value = "{io.quarkus.it.rest.client.HeaderUtil.generateHeader}") })
    String getHeader();

    @SuppressWarnings("unused") // used for header
    default String computeTheHeader() {
        return COMPUTED_VALUE;
    }
}
