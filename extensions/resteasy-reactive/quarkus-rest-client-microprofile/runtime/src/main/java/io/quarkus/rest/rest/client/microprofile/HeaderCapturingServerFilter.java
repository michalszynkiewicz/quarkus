package io.quarkus.rest.rest.client.microprofile;

import javax.ws.rs.container.ContainerRequestContext;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import io.quarkus.arc.Arc;

@SuppressWarnings("unused")
public class HeaderCapturingServerFilter {

    @ServerRequestFilter
    void filter(ContainerRequestContext requestContext) {
        HeaderContainer instance = Arc.container().instance(HeaderContainer.class).get();
        if (instance != null) {
            instance.setContainerRequestContext(requestContext);
        }
    }
}
