package io.quarkus.reactivemessaging.websocket.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.websocket.WsClient;
import io.quarkus.reactivemessaging.websocket.source.app.Consumer;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

// mstodo rainy day scenarios
class WebsocketSourceTest {

    private static Vertx vertx;
    private static WsClient wsClient;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Consumer.class, WsClient.class))
            .withConfigurationResource("websocket-source-test-application.properties");

    @TestHTTPResource("my-ws")
    URI wsSourceUri;

    @Inject
    Consumer consumer;

    @Test
    void shouldPassTextContentAndHeaders() {
        wsClient.connect(wsSourceUri)
                .send("test-message")
                .verify(5, TimeUnit.SECONDS);

        assertThat(consumer.getMessages()).hasSize(1);
        assertThat(consumer.getMessages().get(0).getPayload().toString()).isEqualTo("test-message");
    }

    @AfterEach
    void cleanUp() {
        consumer.getMessages().clear();
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
