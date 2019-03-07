package io.quarkus.smallrye.restclient.runtime;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.specimpl.UnmodifiableMultivaluedMap;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 2/28/19
 */
public class IncomingHeadersProvider implements io.smallrye.restclient.header.IncomingHeadersProvider {
    public static final UnmodifiableMultivaluedMap<String, String> EMPTY_MAP = new UnmodifiableMultivaluedMap<>(
            new MultivaluedHashMap<>());

    /**
     * @return headers incoming in the JAX-RS request, if any
     */
    @Override
    public MultivaluedMap<String, String> getIncomingHeaders() {
        System.out.println("getting incoming headers"); // mstodo remove
        MultivaluedMap<String, String> result = null;

        ResteasyProviderFactory providerFactory = ResteasyProviderFactory.peekInstance();
        if (providerFactory != null) {
            HttpRequest request = (HttpRequest) providerFactory.getContextData(HttpRequest.class);
            if (request != null) {
                result = request.getHttpHeaders().getRequestHeaders();
            }
        }
        return result == null
                ? EMPTY_MAP
                : result;
    }
}
