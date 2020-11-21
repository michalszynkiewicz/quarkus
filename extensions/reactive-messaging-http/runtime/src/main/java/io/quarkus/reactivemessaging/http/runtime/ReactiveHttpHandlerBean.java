package io.quarkus.reactivemessaging.http.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.config.HttpStreamConfig;
import io.quarkus.reactivemessaging.http.runtime.config.ReactiveHttpConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class ReactiveHttpHandlerBean {

    private static final Logger log = Logger.getLogger(ReactiveHttpHandlerBean.class);

    @Inject
    ReactiveHttpConfig config;

    private final Map<String, Bundle<HttpMessage<?>>> httpProcessors = new HashMap<>();

    @PostConstruct
    void init() {
        config.getHttpConfigs()
                .forEach(this::addHttpProcessor);
    }

    void handleHttp(RoutingContext event) {
        String path = event.normalisedPath();
        HttpMethod method = event.request().method();
        String key = key(path, method);
        Bundle<HttpMessage<?>> httpProcessorBundle = httpProcessors.get(key);
        if (httpProcessorBundle != null) {
            MultiEmitter<? super HttpMessage<?>> emitter = httpProcessorBundle.emitter;
            StrictQueueSizeGuard guard = httpProcessorBundle.guard;
            if (guard.prepareToEmit()) {
                try {
                    HttpMessage<Buffer> message = new HttpMessage<>(event.getBody(), new IncomingHttpMetadata(event.request()),
                            () -> event.response().setStatusCode(202).end(),
                            error -> {
                                log.error("Failed to process message.", error);
                                event.response().setStatusCode(500).end("Failed to process ");
                            });
                    emitter.emit(message);
                } catch (Exception any) {
                    guard.dequeue();

                    log.error("Emitting message failed", any);
                    event.response().setStatusCode(500).end();
                }
            } else {
                event.response().setStatusCode(503).end();
            }
        } else {
            event.response().setStatusCode(404).end("No http consumer for the given path and method");
        }
    }

    private void addHttpProcessor(HttpStreamConfig streamConfig) {
        String key = key(streamConfig.path, streamConfig.method);

        // emitter with an unbounded queue, we control the size ourselves, with the guard
        StrictQueueSizeGuard guard = new StrictQueueSizeGuard(streamConfig.bufferSize);
        Bundle<HttpMessage<?>> bundle = new Bundle<>(guard);
        Multi<HttpMessage<?>> processor = Multi.createFrom()
                .<HttpMessage<?>> emitter(bundle::setEmitter, BackPressureStrategy.BUFFER).onItem().invoke(guard::dequeue);
        bundle.setProcessor(processor);

        Bundle<HttpMessage<?>> previousProcessor = httpProcessors.put(key, bundle);
        if (previousProcessor != null) {
            throw new IllegalStateException("Duplicate incoming streams defined for path " + streamConfig.path
                    + " and method " + streamConfig.method);
        }
    }

    private String key(String path, HttpMethod method) {
        return String.format("%s:%s", path, method);
    }

    public Multi<HttpMessage<?>> getProcessor(String path, HttpMethod method) {
        return httpProcessors.get(key(path, method)).processor;
    }

    private static class Bundle<MessageType extends Message<?>> {
        private final StrictQueueSizeGuard guard;
        private Multi<MessageType> processor; // effectively final
        private MultiEmitter<? super MessageType> emitter; // effectively final

        private Bundle(StrictQueueSizeGuard guard) {
            this.guard = guard;
        }

        public void setProcessor(Multi<MessageType> processor) {
            this.processor = processor;
        }

        public void setEmitter(MultiEmitter<? super MessageType> emitter) {
            this.emitter = emitter;
        }
    }
}
