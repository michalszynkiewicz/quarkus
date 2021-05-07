package org.jboss.resteasy.reactive.client.handlers;

import io.smallrye.discovery.ServiceInstance;
import io.smallrye.loadbalancer.LoadBalancer;
import java.net.URI;
import java.util.concurrent.CompletionStage;
import javax.ws.rs.core.UriBuilder;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;

public class ClientDetermineUriHandler implements ClientRestHandler {
    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        UriBuilder uriBuilder = requestContext.getUriBuilder();
        if (requestContext.getLoadBalancer() != null) {
            LoadBalancer loadBalancer = requestContext.getLoadBalancer();
            CompletionStage<ServiceInstance> serviceInstance = loadBalancer.getServiceInstance();
            requestContext.suspend();
            serviceInstance.thenAccept(
                    instance -> {
                        URI newUri = configureUri(uriBuilder, instance);
                        requestContext.setUri(newUri);
                        requestContext.resume();
                    });
        } else {
            requestContext.setUri(uriBuilder.build());
        }
    }

    // MSTODO: cache the results of unwrapping
    private URI configureUri(UriBuilder uri, ServiceInstance serviceInstance) {
        String instance = serviceInstance.getValue();
        int endOfScheme = instance.indexOf(':');

        if (endOfScheme < 1 || instance.length() < endOfScheme + 3 || instance.charAt(endOfScheme + 1) != '/'
                || instance.charAt(endOfScheme + 2) != '/') {
            throw new IllegalArgumentException("Invalid ServiceInstance address returned by the load balancer. " +
                    "Expected <scheme>://host[:port] got " + instance);
        }

        int endOfHostname = instance.indexOf(':', endOfScheme + 1);
        if (endOfHostname > 0) {
            String portAsString = instance.substring(endOfHostname + 1);
            int port;
            try {
                port = Integer.parseInt(portAsString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("");
            }

            return uri.host(instance.substring(endOfScheme + 2, endOfScheme))
                    .scheme(instance.substring(0, endOfScheme))
                    .port(port)
                    .build();
        } else {
            return uri.host(instance.substring(endOfScheme + 2))
                    .scheme(instance.substring(0, endOfScheme))
                    .build();
        }
    }
}
