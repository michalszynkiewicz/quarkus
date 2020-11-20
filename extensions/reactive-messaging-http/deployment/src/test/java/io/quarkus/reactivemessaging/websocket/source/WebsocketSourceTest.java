package io.quarkus.reactivemessaging.websocket.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.VertxFriendlyLock;
import io.quarkus.reactivemessaging.websocket.WsClient;
import io.quarkus.reactivemessaging.websocket.source.app.Consumer;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

class WebsocketSourceTest {

    private static Vertx vertx;
    private static WsClient wsClient;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Consumer.class, WsClient.class, VertxFriendlyLock.class))
            .withConfigurationResource("websocket-source-test-application.properties");

    @TestHTTPResource("my-ws")
    URI wsSourceUri;

    @TestHTTPResource("my-ws-buffer-13")
    URI wsSourceBuffer13Uri;

    @Inject
    Consumer consumer;

    @Test
    void shouldPassTextContentAndHeaders() {
        wsClient.connect(wsSourceUri).send("test-message");

        await("")
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> consumer.getMessages(), hasSize(1));
        String payload = consumer.getMessages().get(0);
        assertThat(payload).isEqualTo("test-message");
    }

    @Test
    void shouldBuffer13IfConfigured() {
        shouldBuffer(13, wsSourceBuffer13Uri);
    }

    @Test
    void shouldBuffer8ByDefault() {
        shouldBuffer(8, wsSourceUri);
    }

    void shouldBuffer(int bufferSize, URI wsUri) {
        WsClient.WsConnection connection = wsClient.connect(wsUri);
        int messagesToSend = 17;
        // 1 message should start being consumed, `messagesToSend` should be buffered, the rest should respond with 503

        consumer.pause();
        ExecutorService executorService = Executors.newFixedThreadPool(messagesToSend);

        for (int i = 0; i < messagesToSend; i++) {
            executorService.submit(() -> connection.send("some-text"));
        }

        Integer expectedFailureCount = messagesToSend - bufferSize - 1;

        await("assert " + expectedFailureCount + " failures")
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> connection.getResponses().stream().filter("BUFFER_OVERFLOW"::equals).count(),
                        equalTo(expectedFailureCount.longValue()));

        consumer.resume();

        await("all processing finished")
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> connection.getResponses().size(), equalTo(messagesToSend));

        assertThat(consumer.getMessages()).hasSize(messagesToSend - expectedFailureCount);
    }

    @AfterEach
    void cleanUp() {
        consumer.clear();
    }

    @BeforeAll
    static void setUp() {
        vertx = Vertx.vertx();
        wsClient = new WsClient(vertx);
    }

    @AfterAll
    static void tearDown() throws InterruptedException {
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        vertx.close(ignored -> shutdownLatch.countDown());
        shutdownLatch.await(10, TimeUnit.SECONDS);
    }
}
