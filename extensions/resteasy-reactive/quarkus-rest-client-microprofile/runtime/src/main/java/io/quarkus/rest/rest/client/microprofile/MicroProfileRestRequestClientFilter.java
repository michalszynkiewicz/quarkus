package io.quarkus.rest.rest.client.microprofile;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;

import io.quarkus.arc.Arc;
import io.smallrye.common.constraint.NotNull;
import io.smallrye.common.constraint.Nullable;

@Priority(Integer.MIN_VALUE)
public class MicroProfileRestRequestClientFilter implements ClientRequestFilter {
    private static final MultivaluedMap<String, String> EMPTY_MAP = new MultivaluedHashMap<>();

    @Nullable
    private final HeaderFiller headerFiller;
    @NotNull
    private final ClientHeadersFactory headersFactory;

    @Nullable
    private final Method method;

    /**
     *
     * @param headerFiller fills headers as specified in @ClientHeaderParam annotations
     * @param headersFactory MP Rest Client headersFactory
     * @param method java method of the JAX-RS interface
     */
    public MicroProfileRestRequestClientFilter(@Nullable HeaderFiller headerFiller,
            @NotNull ClientHeadersFactory headersFactory,
            // TODO: to optimize, add an option to disable passing the method?
            @Nullable Method method) {
        this.headerFiller = headerFiller;
        this.headersFactory = headersFactory;
        this.method = method;
    }

    // for each method, register one such filter
    @Override
    public void filter(ClientRequestContext requestContext) {
        // mutable collection of headers
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        // gather original headers
        requestContext.getHeaders().forEach(
                (key, values) -> headers.put(key, castToListOfStrings(values)));

        // add headers from MP annotations
        if (headerFiller != null) {
            // add headers to a mutable headers collection
            headerFiller.addHeaders(headers);
        }

        MultivaluedMap<String, String> incomingHeaders = MicroProfileRestRequestClientFilter.EMPTY_MAP;
        if (Arc.container().getActiveContext(RequestScoped.class) != null) {
            HeaderContainer headerContainer = Arc.container().instance(HeaderContainer.class).get();
            if (headerContainer != null) {
                incomingHeaders = headerContainer.getHeaders();
            }
        }

        if (headersFactory instanceof DefaultClientHeadersFactoryImpl) {
            // When using the default factory, pass the proposed outgoing headers onto the request context.
            // Propagation with the default factory will then overwrite any values if required.
            headers.forEach((key, values) -> requestContext.getHeaders().put(key, castToListOfObjects(values)));
        }
        headersFactory.update(incomingHeaders, headers)
                .forEach((key, values) -> requestContext.getHeaders().put(key, castToListOfObjects(values)));

        requestContext.setProperty("org.eclipse.microprofile.rest.client.invokedMethod", method);
    }

    private static List<String> castToListOfStrings(List<Object> values) {
        return values.stream()
                .map(val -> val instanceof String
                        ? (String) val
                        : String.valueOf(val))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castToListOfObjects(List<String> values) {
        return (List<Object>) (List<?>) values;
    }

}
