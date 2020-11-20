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
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;
import io.vertx.core.Vertx;

@Connector(QuarkusWebsocketConnector.NAME)
@ConnectorAttribute(name = "path", type = "string", direction = INCOMING, description = "The path of the endpoint", mandatory = true)
@ConnectorAttribute(name = "buffer-size", type = "string", direction = INCOMING, description = "Websocket endpoint buffers messages if a consumer is not able to keep up. This setting specifies the size of the buffer.", defaultValue = QuarkusHttpConnector.DEFAULT_SOURCE_BUFFER_STR)
@ApplicationScoped
public class QuarkusWebsocketConnector implements IncomingConnectorFactory, OutgoingConnectorFactory {
    public static final String NAME = "quarkus-websocket";

    @Inject
    ReactiveWebsocketHandlerBean handlerBean;

    @Inject
    SerializerFactoryBase serializerFactory;

    @Inject
    Vertx vertx;

    @Override
    public PublisherBuilder<WebsocketMessage<?>> getPublisherBuilder(Config config) {
        String path = getRequiredAttribute(config, "path", String.class);

        Multi<WebsocketMessage<?>> processor = handlerBean.getProcessor(path);
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
                .orElseThrow(() -> new IllegalArgumentException("'" + attributeName + "' must be set for a quarkus websocket connector"));
    }
}
