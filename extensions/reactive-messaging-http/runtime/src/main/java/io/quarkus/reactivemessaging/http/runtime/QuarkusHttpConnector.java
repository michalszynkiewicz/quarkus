package io.quarkus.reactivemessaging.http.runtime;

import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.INCOMING;
import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.INCOMING_AND_OUTGOING;
import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.OUTGOING;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

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
import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.serializers.SerializerFactoryBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;

@ConnectorAttribute(name = "url", type = "string", direction = OUTGOING, description = "The targeted URL", mandatory = true)
@ConnectorAttribute(name = "serializer", type = "string", direction = OUTGOING, description = "Message serializer")

// mstodo descriptions
@ConnectorAttribute(name = "jitter", type = "string", direction = OUTGOING, description = "Configures the random factor when using back-off")
@ConnectorAttribute(name = "delay", type = "string", direction = OUTGOING, description = "Configures a back-off delay between to attempt to re-subscribe. A random factor (jitter) is applied to increase the delay when several failures happen.", defaultValue = QuarkusHttpConnector.DEFAULT_JITTER)
@ConnectorAttribute(name = "maxAttempts", type = "int", direction = OUTGOING, description = "The number of attempts, must be greater than zero", defaultValue = QuarkusHttpConnector.DEFAULT_MAX_ATTEMPTS_STR)

@ConnectorAttribute(name = "method", type = "string", direction = INCOMING_AND_OUTGOING, description = "The HTTP method (either `POST` or `PUT`)", defaultValue = "POST")
@ConnectorAttribute(name = "path", type = "string", direction = INCOMING, description = "The path of the endpoint", mandatory = true)
@ConnectorAttribute(name = "buffer-size", type = "string", direction = INCOMING, description = "HTTP endpoint buffers messages if a consumer is not able to keep up. This setting specifies the size of the buffer.", defaultValue = QuarkusHttpConnector.DEFAULT_SOURCE_BUFFER_STR)

@Connector(QuarkusHttpConnector.NAME)
@ApplicationScoped
public class QuarkusHttpConnector implements IncomingConnectorFactory, OutgoingConnectorFactory {
    private static final Logger log = Logger.getLogger(QuarkusHttpConnector.class);

    static final String DEFAULT_JITTER = "0.5";
    static final String DEFAULT_MAX_ATTEMPTS_STR = "1";
    static final Integer DEFAULT_MAX_ATTEMPTS = Integer.valueOf(DEFAULT_MAX_ATTEMPTS_STR);

    static final String DEFAULT_SOURCE_BUFFER_STR = "8";
    public static final Integer DEFAULT_SOURCE_BUFFER = Integer.valueOf(DEFAULT_SOURCE_BUFFER_STR);

    public static final String NAME = "quarkus-http";

    @Inject
    ReactiveHttpHandlerBean handlerBean;

    @Inject
    Vertx vertx;

    @Inject
    SerializerFactoryBase serializerFactory;

    @Override
    public PublisherBuilder<HttpMessage<?>> getPublisherBuilder(Config config) {
        String path = config.getOptionalValue("path", String.class)
                .orElseThrow(() -> new IllegalArgumentException("The `path` must be set"));
        HttpMethod method = getMethod(config);

        Multi<HttpMessage<?>> processor = handlerBean.getProcessor(path, method);

        return ReactiveStreams.fromPublisher(processor);
    }

    private HttpMethod getMethod(Config config) {
        String methodAsString = config.getOptionalValue("method", String.class)
                .map(s -> s.toUpperCase(Locale.getDefault()))
                .orElse("POST");
        try {
            return HttpMethod.valueOf(methodAsString);
        } catch (IllegalArgumentException e) {
            String error = "Unsupported HTTP method: " + methodAsString + ". The supported methods are: "
                    + Arrays.toString(HttpMethod.values());
            log.warn(error, e);
            throw new IllegalArgumentException(error);
        }
    }

    @Override
    public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(Config config) {
        String url = config.getValue("url", String.class);
        String method = getMethod(config).name();
        String serializer = config.getOptionalValue("serializer", String.class).orElse(null);
        Optional<Duration> delay = config.getOptionalValue("delay", Duration.class);
        String jitterAsString = config.getOptionalValue("jitter", String.class).orElse(DEFAULT_JITTER);
        Integer maxAttempts = config.getOptionalValue("maxAttempts", Integer.class).orElse(DEFAULT_MAX_ATTEMPTS);

        double jitter;
        try {
            jitter = Double.valueOf(jitterAsString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed to parse jitter value '" + jitterAsString + "' to a double.");
        }

        return new HttpSink(vertx, method, url, serializer, maxAttempts, jitter, delay, serializerFactory).sink();
    }
}
