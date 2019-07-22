package io.quarkus.reactivemessaging.http;

import java.util.Arrays;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.jboss.logging.Logger;

import io.reactivex.processors.BehaviorProcessor;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 09/07/2019
 */
@Connector(QuarkusHttpConnector.NAME)
@ApplicationScoped
public class QuarkusHttpConnector implements IncomingConnectorFactory {

    private static Logger log = Logger.getLogger(QuarkusHttpConnector.class);

    static final String NAME = "quarkus-http";

    @Inject
    Event<HttpProcessorCreated> processorCreatedEvent;

    @Override
    public PublisherBuilder<HttpMessage<?>> getPublisherBuilder(Config config) {

        String path = config.getOptionalValue("path", String.class)
                .orElseThrow(() -> new IllegalArgumentException("The `path` must be set"));
        String contentType = config.getOptionalValue("content-type", String.class).orElse("text/plain");

        HttpMethod method = getMethod(config);

        BehaviorProcessor<HttpMessage<?>> processor = buildAndPropagateProcessor(path, method, contentType);

        return new HttpSource().getSource(processor);
    }

    private HttpMethod getMethod(Config config) {
        String methodAsString = config.getOptionalValue("method", String.class)
                .map(String::toUpperCase)
                .orElseThrow(() -> new IllegalArgumentException("The `method` must be set"));
        try {
            return HttpMethod.valueOf(methodAsString);
        } catch (IllegalArgumentException e) {
            String error = "Unsupported HTTP method: " + methodAsString + ". The supported methods are: "
                    + Arrays.toString(HttpMethod.values());
            log.warn(error, e);
            throw new IllegalArgumentException(error);
        }
    }

    private BehaviorProcessor<HttpMessage<?>> buildAndPropagateProcessor(String path, HttpMethod method, String contentType) {
        BehaviorProcessor<HttpMessage<?>> processor = BehaviorProcessor.create();
        Deserializer<?> deserializer = createDeserializer(contentType);
        processorCreatedEvent.fire(new HttpProcessorCreated(path, method, processor, deserializer));
        return processor;
    }

    private Deserializer<?> createDeserializer(String contentType) {
        contentType = contentType.toLowerCase();
        if (contentType.contains("text")) {
            return Deserializer.STRING;
        }
        if (contentType.contains("json")) {
            return Deserializer.JSON;
        }

        return Deserializer.BYTE_ARRAY;
    }
}
