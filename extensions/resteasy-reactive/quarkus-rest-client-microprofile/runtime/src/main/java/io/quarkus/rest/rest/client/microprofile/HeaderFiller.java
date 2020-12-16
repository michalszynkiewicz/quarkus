package io.quarkus.rest.rest.client.microprofile;

import javax.ws.rs.core.MultivaluedMap;

public interface HeaderFiller {
    void addHeaders(MultivaluedMap<String, String> headers);
}
