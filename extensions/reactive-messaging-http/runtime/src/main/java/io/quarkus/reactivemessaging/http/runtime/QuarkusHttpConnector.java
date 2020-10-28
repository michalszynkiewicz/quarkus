package io.quarkus.reactivemessaging.http.runtime;

import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.INCOMING;
import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.INCOMING_AND_OUTGOING;
import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.OUTGOING;

import java.util.Arrays;
import java.util.Locale;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.jboss.logging.Logger;

import io.reactivex.processors.BehaviorProcessor;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 28/08/2019
 */
@Connector(QuarkusHttpConnector.NAME)

// mstodo go through these, they're copied from smallrye RM
@ConnectorAttribute(name = "url", type = "string", direction = OUTGOING, description = "The targeted URL", mandatory = true)
//@ConnectorAttribute(name = "converter", type = "string", direction = OUTGOING, description = "The converter classname used to serialized the outgoing message in the HTTP body")

@ConnectorAttribute(name = "method", type = "string", direction = INCOMING_AND_OUTGOING, description = "The HTTP method (either `POST` or `PUT`)", defaultValue = "POST")
@ConnectorAttribute(name = "path", type = "string", direction = INCOMING, description = "The path of the endpoint", mandatory = true)
// mstodo end

@ApplicationScoped
public class QuarkusHttpConnector implements IncomingConnectorFactory, OutgoingConnectorFactory {
    private static Logger log = Logger.getLogger(QuarkusHttpConnector.class);

    public static final String NAME = "quarkus-http";

    @Inject
    ReactiveHttpHandlerBean handlerBean;

    @Inject
    Vertx vertx;

    @Override
    public PublisherBuilder<HttpMessage> getPublisherBuilder(Config config) {
        String path = config.getOptionalValue("path", String.class)
                .orElseThrow(() -> new IllegalArgumentException("The `path` must be set"));
        HttpMethod method = getMethod(config);

        BehaviorProcessor<HttpMessage> processor = handlerBean.getProcessor(path, method);
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
    public SubscriberBuilder<HttpMessage, Void> getSubscriberBuilder(Config config) {
        return new HttpSink(vertx, getMethod(config).name(), config.getValue("url", String.class)).sink();
    }
}
