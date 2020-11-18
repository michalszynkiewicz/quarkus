package io.quarkus.reactivemessaging.http.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.config.HttpStreamConfig;
import io.quarkus.reactivemessaging.http.runtime.config.ReactiveHttpConfig;
import io.quarkus.reactivemessaging.http.runtime.config.WebsocketStreamConfig;
import io.reactivex.processors.BehaviorProcessor;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

// mstodo separate http from websocket
@Singleton
public class ReactiveHttpHandlerBean {

    private static final Logger log = Logger.getLogger(ReactiveHttpHandlerBean.class);

    @Inject
    ReactiveHttpConfig config;

    private final Map<String, Bundle> processors = new HashMap<>();
    private final Map<String, BehaviorProcessor<WebsocketMessage<?>>> websocketProcessors = new HashMap<>();

    @PostConstruct
    void init() {
        config.getHttpConfigs()
                .forEach(this::addHttpProcessor);
        config.getWebsocketConfigs()
                .forEach(this::addWebsocketProcessor);
    }

    void handleWebsocket(RoutingContext event) {
        //  mstodo drop it, websockets don't have headers, me thinks
        // mstodo them should have a different message
        String path = event.normalisedPath();
        // mstodo maybe messages should be generic and have the headers, etc in some context map?
        BehaviorProcessor<WebsocketMessage<?>> processor = websocketProcessors.get(path);
        if (processor != null) {
            event.request().toWebSocket(websocket -> {
                if (websocket.failed()) {
                    // mstodo handle this
                } else {
                    ServerWebSocket result = websocket.result();
                    result.handler(
                            b -> processor.onNext(new WebsocketMessage<>(b))); // mstodo this sounds wrong
                }
            });
        } else {
            event.response().setStatusCode(404).end("Handler found but no config for the current path found");
        }
    }

    BehaviorProcessor<WebsocketMessage<?>> getWebsocketProcessor(String path) {
        BehaviorProcessor<WebsocketMessage<?>> processor = websocketProcessors.get(path);
        if (processor == null) {
            throw new IllegalStateException("No incoming stream defined for path " + path);
        }
        return processor;
    }

    void handleHttp(RoutingContext event) {
        // mstodo remove printlns
        System.out.println("handling an event");
        String path = event.normalisedPath();
        HttpMethod method = event.request().method();
        String key = key(path, method);
        Bundle httpProcessorBundle = processors.get(key);
        if (httpProcessorBundle != null) {
            MultiEmitter<? super HttpMessage<?>> emitter = httpProcessorBundle.emitter;
            StrictQueueSizeGuard guard = httpProcessorBundle.guard;
            HttpMessage<Buffer> message = new HttpMessage<>(event.getBody(), event.request().headers(),
                    () -> {
                        event.response().setStatusCode(202).end();
                        guard.dequeue();
                    },
                    error -> {
                        if (error instanceof ReactiveHttpException) {
                            event.response().setStatusCode(((ReactiveHttpException) error).getStatusCode())
                                    .end(error.getMessage()); // mstodo proper handling
                        } else {
                            event.response().setStatusCode(500).end("Failed to process ");
                            log.error("Failed to process messsage.", error);
                        }
                    });
            System.out.println("created a message, checking if guard allows emission");

            if (guard.prepareToEmit()) {
                try {
                    System.out.println("it's a go!");
                    emitter.emit(message);
                    //                    mstodo: seems this was too early
                    //                    event.response().setStatusCode(202).end();
                    System.out.println("emit called successfully");
                } catch (Exception any) {
                    System.out.println("emit failed, will dequeue");
                    guard.dequeue();
                    log.error("Emitting message failed", any);
                    event.response().setStatusCode(500).end();
                }
            } else {
                event.response().setStatusCode(503).end();
            }
            //            try {
            //                processor.onNext(message);
            //            } catch (Exception whatever) {
            //                whatever.printStackTrace(); // mstodo remvoe
            //                event.response().setStatusCode(500);
            //                return;
            //            }
        } else {
            System.out.println("bundle not found, replying with 404");
            event.response().setStatusCode(404).end("No http consumer for the given path and method");
        }
    }

    private void addWebsocketProcessor(WebsocketStreamConfig streamConfig) {
        BehaviorProcessor<WebsocketMessage<?>> processor = BehaviorProcessor.create();
        BehaviorProcessor<?> previousProcessor = websocketProcessors.put(streamConfig.path, processor);
        if (previousProcessor != null) {
            throw new IllegalStateException("Duplicate incoming streams defined for path " + streamConfig.path);
        }
    }

    private void addHttpProcessor(HttpStreamConfig streamConfig) {
        String key = key(streamConfig.path, streamConfig.method);

        // emitter with an unbounded queue, we control the size ourselves, with the guard
        StrictQueueSizeGuard guard = new StrictQueueSizeGuard(streamConfig.bufferSize);
        Bundle bundle = new Bundle(guard);
        Multi<HttpMessage<?>> processor = Multi.createFrom().<HttpMessage<?>> emitter(emitter -> bundle.setEmitter(emitter),
                BackPressureStrategy.BUFFER).onOverflow().buffer();
        bundle.setProcessor(processor);
        // mstodo no subscribe until the consumer is connected
        //        httpEmitter.subscribe().with(consumer.consumer());

        //        Multi<HttpMessage<?>> httpMessageEmitter =
        //        Multi<HttpMessage<?>> httpEmitter = Multi.createFrom()
        //                .<HttpMessage<?>> emitter(emitter -> emitters.put(key, emitter)/* , BackPressureStrategy.BUFFER */);
        //        MultiOverflow<HttpMessage<?>> multiOverflow = httpEmitter
        //                .onOverflow();
        //        Multi<HttpMessage<?>> httpMessageEmitter = Infrastructure
        //                .onMultiCreation(new MultiOnOverflowBufferOp<>(httpEmitter,
        //                        ParameterValidation.positive(streamConfig.bufferSize, "size"),
        //                        false,
        //                        x -> {
        //                            x.nack(new BackPressureFailure("buffer overflow"));
        //                            System.out.println("throwing out " + x);
        // do nothing
        //                        }));
        //        multiOverflow.buffer(streamConfig.bufferSize);

        Bundle previousProcessor = processors.put(key, bundle);
        if (previousProcessor != null) {
            throw new IllegalStateException("Duplicate incoming streams defined for path " + streamConfig.path
                    + " and method " + streamConfig.method);
        }
    }

    private String key(String path, HttpMethod method) {
        return String.format("%s:%s", path, method);
    }

    public Multi<HttpMessage<?>> getProcessor(String path, HttpMethod method) {
        return processors.get(key(path, method)).processor;
    }

    private static class Bundle {
        private final StrictQueueSizeGuard guard;
        private Multi<HttpMessage<?>> processor; // effectively final
        private MultiEmitter<? super HttpMessage<?>> emitter; // effectively final

        private Bundle(StrictQueueSizeGuard guard) {
            this.guard = guard;
        }

        public void setProcessor(Multi<HttpMessage<?>> processor) {
            this.processor = processor;
        }

        public void setEmitter(MultiEmitter<? super HttpMessage<?>> emitter) {
            this.emitter = emitter;
        }
    }
}
