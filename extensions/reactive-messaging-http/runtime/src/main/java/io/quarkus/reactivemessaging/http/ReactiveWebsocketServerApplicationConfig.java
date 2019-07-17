package io.quarkus.reactivemessaging.http;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 15/07/2019
 */
@ApplicationScoped
public class ReactiveWebsocketServerApplicationConfig implements ServerApplicationConfig {

    private final Map<Class<? extends Endpoint>, List<ServerEndpointConfig>> configs = new ConcurrentHashMap<>();
    private final Map<Class<? extends Endpoint>, Mode> modes = new ConcurrentHashMap<>();
    // mstodo register paths

    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
        if (endpointClasses == null) {
            return Collections.emptySet();
        }

        return endpointClasses.stream()
                .flatMap(c -> configs.getOrDefault(c, Collections.emptyList()).stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        return null;
    }

    private enum Mode {
        TEXT,
        BINARY
    }
}
