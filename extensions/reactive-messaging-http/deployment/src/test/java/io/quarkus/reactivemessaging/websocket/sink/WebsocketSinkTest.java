package io.quarkus.reactivemessaging.websocket.sink;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.websocket.sink.app.WsEndpoint;
import io.quarkus.reactivemessaging.websocket.sink.app.WsRepeater;
import io.quarkus.test.QuarkusUnitTest;

// mstodo better tests
class WebsocketSinkTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(WsEndpoint.class, WsRepeater.class))
            .withConfigurationResource("websocket-sink-test-application.properties");

    @Inject
    WsEndpoint wsEndpoint;

    @Test
    void shouldPassThroughHttpToWebsocketSink() {
        // @formatter:off
        given()
                .body("some-text-from-ws")
        .when()
                .post("/my-http-source")
        .then()
                .statusCode(202);
        // @formatter:on
        assertThat(wsEndpoint.getMessages()).hasSize(1);
        assertThat(wsEndpoint.getMessages().get(0)).isEqualTo("{\"foo\": \"bar\"}");
    }

    @AfterEach
    void cleanUp() {
        wsEndpoint.getMessages().clear();
    }

}
