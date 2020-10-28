package io.quarkus.reactivemessaging.websocket;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;

public class WsClient {

    private final Vertx vertx;

    public WsClient(Vertx vertx) {
        this.vertx = vertx;
    }

    public WsConnection connect(URI uri) {
        CompletableFuture<WebSocket> webSocket = new CompletableFuture<>();
        vertx.createHttpClient().webSocket(uri.getPort(), uri.getHost(), uri.getPath(), ws -> {
            if (ws.succeeded()) {
                webSocket.complete(ws.result());
            } else {
                webSocket.completeExceptionally(ws.cause());
            }
        });
        return new WsConnection(webSocket);
    }

    public static class WsConnection {

        private CompletableFuture<WebSocket> ws;

        private WsConnection(CompletableFuture<WebSocket> ws) {
            this.ws = ws;
        }

        public WsConnection verify(int timeout, TimeUnit timeUnit) {
            try {
                ws.get(timeout, timeUnit);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                throw new RuntimeException("Websocket client failed", e.getCause());
            }
            return this;
        }

        public WsConnection send(String message) {
            ws = ws.thenApply(
                    webSocket -> {
                        try {
                            webSocket.writeTextMessage(message);
                        } catch (Exception any) {
                            throw new RuntimeException("Failed to send message " + message, any);
                        }
                        return webSocket;
                    });
            return this;
        }

    }
}
