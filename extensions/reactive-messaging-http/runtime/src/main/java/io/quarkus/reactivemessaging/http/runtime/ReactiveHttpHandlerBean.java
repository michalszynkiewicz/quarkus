package io.quarkus.reactivemessaging.http.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.reactivemessaging.http.runtime.config.HttpStreamConfig;
import io.quarkus.reactivemessaging.http.runtime.config.ReactiveHttpConfig;
import io.quarkus.reactivemessaging.http.runtime.config.WebsocketStreamConfig;
import io.reactivex.processors.BehaviorProcessor;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

// mstodo that needed?
@Singleton
public class ReactiveHttpHandlerBean {

    @Inject
    ReactiveHttpConfig config;

    private final Map<String, BehaviorProcessor<HttpMessage<?>>> processors = new HashMap<>();
    private final Map<String, BehaviorProcessor<WebsocketMessage<?>>> websocketProcessors = new HashMap<>();

    void handleHttp(RoutingContext event) {
        String path = event.normalisedPath();
        HttpMethod method = event.request().method();
        BehaviorProcessor<HttpMessage<?>> processor = processors.get(key(path, method));
        if (processor != null) {
            processor.onNext(new HttpMessage<>(event.getBody(), event.request().headers()));
            event.response().setStatusCode(202).end();
        } else {
            event.response().setStatusCode(404).end("Handler found but no config for the current path/config pair");
        }
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

    BehaviorProcessor<HttpMessage<?>> getProcessor(String path, HttpMethod method) {
        BehaviorProcessor<HttpMessage<?>> processor = processors.get(key(path, method));
        if (processor == null) {
            throw new IllegalStateException("No incoming stream defined for path " + path + " and method " + method);
        }
        return processor;
    }

    BehaviorProcessor<WebsocketMessage<?>> getWebsocketProcessor(String path) {
        BehaviorProcessor<WebsocketMessage<?>> processor = websocketProcessors.get(path);
        if (processor == null) {
            throw new IllegalStateException("No incoming stream defined for path " + path);
        }
        return processor;
    }

    @PostConstruct
    void init() {
        config.getHttpConfigs()
                .forEach(this::addHttpProcessor);
        config.getWebsocketConfigs()
                .forEach(this::addWebsocketProcessor);
    }

    private void addWebsocketProcessor(WebsocketStreamConfig streamConfig) {
        BehaviorProcessor<WebsocketMessage<?>> processor = BehaviorProcessor.create();
        BehaviorProcessor<?> previousProcessor = websocketProcessors.put(streamConfig.path, processor);
        if (previousProcessor != null) {
            throw new IllegalStateException("Duplicate incoming streams defined for path " + streamConfig.path);
        }
    }

    private void addHttpProcessor(HttpStreamConfig streamConfig) {
        BehaviorProcessor<HttpMessage<?>> processor = BehaviorProcessor.create();
        BehaviorProcessor<?> previousProcessor = processors.put(key(streamConfig.path, streamConfig.method), processor);
        if (previousProcessor != null) {
            throw new IllegalStateException("Duplicate incoming streams defined for path " + streamConfig.path
                    + " and method " + streamConfig.method);
        }
    }

    private String key(String path, HttpMethod method) {
        return String.format("%s:%s", path, method);
    }
}
