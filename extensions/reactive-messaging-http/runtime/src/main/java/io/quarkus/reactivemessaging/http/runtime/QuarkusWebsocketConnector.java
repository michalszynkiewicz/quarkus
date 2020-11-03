package io.quarkus.reactivemessaging.http.runtime;

import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.INCOMING;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.quarkus.reactivemessaging.http.runtime.serializers.SerializerFactoryBase;
import io.reactivex.processors.BehaviorProcessor;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;
import io.vertx.core.Vertx;

@Connector(QuarkusWebsocketConnector.NAME)
@ConnectorAttribute(name = "path", type = "string", direction = INCOMING, description = "The path of the endpoint", mandatory = true)
@ApplicationScoped
public class QuarkusWebsocketConnector implements IncomingConnectorFactory, OutgoingConnectorFactory {
    public static final String NAME = "quarkus-websocket";

    @Inject
    ReactiveHttpHandlerBean handlerBean;

    @Inject
    SerializerFactoryBase serializerFactory;

    @Inject
    Vertx vertx;

    @Override
    public PublisherBuilder<WebsocketMessage<?>> getPublisherBuilder(Config config) {
        String path = getRequiredAttribute(config, "path", String.class);

        BehaviorProcessor<WebsocketMessage<?>> processor = handlerBean.getWebsocketProcessor(path);
        return ReactiveStreams.fromPublisher(processor);
    }

    @Override
    public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(Config config) {
        String serializer = config.getOptionalValue("serializer", String.class).orElse(null);
        URI url = getRequiredAttribute(config, "url", URI.class);
        return new WebsocketSink(vertx, url, serializer, serializerFactory).sink();
    }

    private <T> T getRequiredAttribute(Config config, String attributeName, Class<T> type) {
        return config.getOptionalValue(attributeName, type)
                .orElseThrow(() -> new IllegalArgumentException("The '" + attributeName + "' must be set")); //mstodo better error message!!
    }
}
