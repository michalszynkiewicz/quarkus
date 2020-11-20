package io.quarkus.reactivemessaging.http.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.config.ReactiveHttpConfig;
import io.quarkus.reactivemessaging.http.runtime.config.WebsocketStreamConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

// mstodo separate http from websocket
@Singleton
public class ReactiveWebsocketHandlerBean {

    private static final Logger log = Logger.getLogger(ReactiveWebsocketHandlerBean.class);

    @Inject
    ReactiveHttpConfig config;

    private final Map<String, Bundle<WebsocketMessage<?>>> websocketProcessors = new HashMap<>();

    @PostConstruct
    void init() {
        config.getWebsocketConfigs()
                .forEach(this::addWebsocketProcessor);
    }

    void handleWebsocket(RoutingContext event) {
        String path = event.normalisedPath();
        Bundle<WebsocketMessage<?>> bundle = websocketProcessors.get(path);
        if (bundle != null) {
            MultiEmitter<? super WebsocketMessage<?>> emitter = bundle.emitter;
            StrictQueueSizeGuard guard = bundle.guard;
            event.request().toWebSocket(
                    websocket -> {
                        if (websocket.failed()) {
                            log.error("failed to connect websocket", websocket.cause());
                        } else {
                            ServerWebSocket serverWebSocket = websocket.result();
                            serverWebSocket.handler(
                                    b -> {
                                        if (guard.prepareToEmit()) {
                                            try {
                                                emitter.emit(new WebsocketMessage<>(b, () -> {
                                                    serverWebSocket.write(Buffer.buffer("ACK"));
                                                }, error -> {
                                                    log.error("Failed to process message.", error);
                                                    serverWebSocket.write(Buffer.buffer("Failed to process message"));
                                                    // mstodo some error message for the client? exception mapper would be best...
                                                }));
                                            } catch (Exception any) {
                                                guard.dequeue();
                                                log.error("Emitting message failed", any);
                                            }
                                        } else {
                                            serverWebSocket.write(Buffer.buffer("BUFFER_OVERFLOW"));
                                        }
                                    });
                        }
                    });
        } else {
            event.response().setStatusCode(404).end("Handler found but no config for the current path found");
        }
    }

    private void addWebsocketProcessor(WebsocketStreamConfig streamConfig) {
        // emitter with an unbounded queue, we control the size ourselves, with the guard
        StrictQueueSizeGuard guard = new StrictQueueSizeGuard(streamConfig.bufferSize);
        Bundle<WebsocketMessage<?>> bundle = new Bundle<>(guard);

        Multi<WebsocketMessage<?>> processor = Multi.createFrom()
                .<WebsocketMessage<?>> emitter(bundle::setEmitter, BackPressureStrategy.BUFFER)
                .onItem().invoke(guard::dequeue);
        bundle.setProcessor(processor);

        Bundle<WebsocketMessage<?>> previousProcessor = websocketProcessors.put(streamConfig.path, bundle);
        if (previousProcessor != null) {
            throw new IllegalStateException("Duplicate incoming streams defined for path " + streamConfig.path);
        }
    }

    Multi<WebsocketMessage<?>> getProcessor(String path) {
        Bundle<WebsocketMessage<?>> bundle = websocketProcessors.get(path);
        if (bundle == null) {
            throw new IllegalStateException("No incoming stream defined for path " + path);
        }
        return bundle.processor;
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
