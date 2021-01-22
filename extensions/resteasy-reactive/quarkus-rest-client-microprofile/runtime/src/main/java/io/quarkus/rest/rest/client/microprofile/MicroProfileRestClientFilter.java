package io.quarkus.rest.rest.client.microprofile;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;

import io.smallrye.common.constraint.NotNull;
import io.smallrye.common.constraint.Nullable;

// mstodo a test for it that also runs in native
@Priority(Integer.MIN_VALUE)
public class MicroProfileRestClientFilter implements ClientRequestFilter {
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
    public MicroProfileRestClientFilter(@Nullable HeaderFiller headerFiller,
            @NotNull ClientHeadersFactory headersFactory,
            // mstodo provide method only if some property is set
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
        // mstodo
        //        @SuppressWarnings("unchecked")
        //        MultivaluedMap<String, String> containerHeaders = (MultivaluedMap<String, String>) requestContext
        //                .getProperty(MpClientInvocation.CONTAINER_HEADERS);
        //        if (containerHeaders == null)
        //            containerHeaders = EMPTY_MAP;
        //        // stupid final rules
        //        MultivaluedMap<String, String> incomingHeaders = containerHeaders;

        if (headersFactory instanceof DefaultClientHeadersFactoryImpl) {
            // When using the default factory, pass the proposed outgoing headers onto the request context.
            // Propagation with the default factory will then overwrite any values if required.
            headers.forEach((key, values) -> requestContext.getHeaders().put(key, castToListOfObjects(values)));
        }

        // mstodo take incoming headers from a server side request filter
        MultivaluedHashMap<String, String> TODO_PASS_INCOMING_HEADERS = new MultivaluedHashMap<>();
        headersFactory.update(TODO_PASS_INCOMING_HEADERS, headers)
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
