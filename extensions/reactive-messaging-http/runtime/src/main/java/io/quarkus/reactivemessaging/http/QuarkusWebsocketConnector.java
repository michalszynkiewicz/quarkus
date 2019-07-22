package io.quarkus.reactivemessaging.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;

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

    @Inject
    Event<WebsocketProcessorCreated> processorCreatedEvent;

    @Override
    public PublisherBuilder<WebsocketMessage<?>> getPublisherBuilder(Config config) {
        BehaviorProcessor<WebsocketMessage<?>> processor = BehaviorProcessor.create();

        String path = config.getOptionalValue("path", String.class)
                .orElseThrow(() -> new IllegalArgumentException("The `path` must be set for a websocket connector"));

        WebsocketProcessorCreated event = new WebsocketProcessorCreated(path, processor);
        System.out.println("firing processor event: " + event);
        processorCreatedEvent.fire(event);

        return new WebsocketSource().getSource(processor);
    }
}
