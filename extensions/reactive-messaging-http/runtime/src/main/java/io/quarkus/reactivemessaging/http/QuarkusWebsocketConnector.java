package io.quarkus.reactivemessaging.http;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.reactivestreams.Processor;

import io.reactivex.processors.BehaviorProcessor;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 15/07/2019
 */
@Connector(QuarkusWebsocketConnector.NAME)
@ApplicationScoped
public class QuarkusWebsocketConnector implements IncomingConnectorFactory {
    static final String NAME = "quarkus-websocket";
    private Map<String, BehaviorProcessor<WebsocketMessage<?>>> processors = new ConcurrentHashMap<>();

    @Override
    public PublisherBuilder<WebsocketMessage<?>> getPublisherBuilder(Config config) {
        BehaviorProcessor<WebsocketMessage<?>> processor = BehaviorProcessor.create();

        String path = config.getOptionalValue("path", String.class)
                .orElseThrow(() -> new IllegalArgumentException("The `path` must be set for a websocket connector"));

        processors.put(path, processor);

        return new WebsocketSource().getSource(processor);
    }

    public Processor<WebsocketMessage<?>, WebsocketMessage<?>> getProcessor(String path) {
        return processors.get(path);
    }

    public Set<String> getPaths() {
        return processors.keySet();
    }
}
