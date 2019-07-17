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
 *         Date: 09/07/2019
 */
@Connector(QuarkusHttpConnector.NAME)
@ApplicationScoped
public class QuarkusHttpConnector implements IncomingConnectorFactory {

    static final String NAME = "quarkus-http";

    private Map<HttpInputDescriptor, BehaviorProcessor<HttpMessage<?>>> processors = new ConcurrentHashMap<>();

    @Override
    public PublisherBuilder<HttpMessage<?>> getPublisherBuilder(Config config) {
        BehaviorProcessor<HttpMessage<?>> processor = BehaviorProcessor.create();

        String path = config.getOptionalValue("path", String.class)
                .orElseThrow(() -> new IllegalArgumentException("The `path` must be set"));

        String method = config.getOptionalValue("method", String.class)
                .map(String::toUpperCase)
                .orElseThrow(() -> new IllegalArgumentException("The `method` must be set"));

        processors.put(new HttpInputDescriptor(path, method), processor);

        PublisherBuilder<HttpMessage<?>> source = new HttpSource().getSource(processor);
        return source;
    }

    public Processor<HttpMessage<?>, HttpMessage<?>> getProcessor(HttpInputDescriptor httpInputDescriptor) {
        return processors.get(httpInputDescriptor);
    }

    public Set<HttpInputDescriptor> getProcessors() {
        return processors.keySet();
    }
}
