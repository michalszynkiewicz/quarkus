package io.quarkus.reactivemessaging.http.runtime;

import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.INCOMING;
import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.INCOMING_AND_OUTGOING;

import java.util.Arrays;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.logging.Logger;

import io.reactivex.processors.BehaviorProcessor;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;
import io.vertx.core.http.HttpMethod;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 28/08/2019
 */
@Connector(QuarkusHttpConnector.NAME)

// mstodo go through these, they're copied from smallrye RM
// @ConnectorAttribute(name = "url", type = "string", direction = OUTGOING, description = "The targeted URL", mandatory = true)
@ConnectorAttribute(name = "method", type = "string", direction = INCOMING_AND_OUTGOING, description = " The HTTP method (either `POST` or `PUT`)", defaultValue = "POST")
//@ConnectorAttribute(name = "converter", type = "string", direction = OUTGOING, description = "The converter classname used to serialized the outgoing message in the HTTP body")

@ConnectorAttribute(name = "path", type = "string", direction = INCOMING, description = "Endpoint of the ", mandatory = true)
//
//@ConnectorAttribute(name = "host", type = "string", direction = INCOMING, description = "the host (interface) on which the server is opened", defaultValue = "0.0.0.0")
//@ConnectorAttribute(name = "port", type = "int", direction = INCOMING, description = "the port", defaultValue = "8080")
// mstodo end

@ApplicationScoped
public class QuarkusHttpConnector implements IncomingConnectorFactory {
    private static Logger log = Logger.getLogger(QuarkusHttpConnector.class);

    public static final String NAME = "quarkus-http";

    @Inject
    ReactiveHttpHandlerBean handlerBean;

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
}
