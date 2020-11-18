package io.quarkus.reactivemessaging.websocket.sink;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.websocket.sink.app.WsEndpoint;
import io.quarkus.reactivemessaging.websocket.sink.app.WsRepeater;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

// mstodo tests for backpressure, http and websocket
// mstodo backpressure for websocketsource
class WebsocketSinkTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(WsEndpoint.class, WsRepeater.class))
            .withConfigurationResource("websocket-sink-test-application.properties");

    @Inject
    WsEndpoint wsEndpoint;

    @Test
    void shouldSerializerBuffer() {
        triggerMessage(WsRepeater.BUFFER);
        await().atMost(1, TimeUnit.SECONDS)
                .until(() -> wsEndpoint.getMessages(), hasSize(1));
        assertThat(wsEndpoint.getMessages().get(0)).isEqualTo("{\"foo\": \"bar\"}");
    }

    @Test
    void shouldSerializeJsonObject() {
        triggerMessage(WsRepeater.JSON_OBJECT);
        await().atMost(1, TimeUnit.SECONDS)
                .until(() -> wsEndpoint.getMessages(), hasSize(1));
        assertThat(new JsonObject(wsEndpoint.getMessages().get(0))).isEqualTo(new JsonObject("{\"jsonFoo\": \"jsonBar\"}"));
    }

    @Test
    void shouldSerializeJsonArray() {
        triggerMessage(WsRepeater.JSON_ARRAY);
        await().atMost(1, TimeUnit.SECONDS)
                .until(() -> wsEndpoint.getMessages(), hasSize(1));
        assertThat(new JsonArray(wsEndpoint.getMessages().get(0)))
                .isEqualTo(new JsonArray().add(new JsonObject().put("arrFoo", "arrBar")));
    }

    @Test
    void shouldSerializeString() {
        triggerMessage(WsRepeater.STRING);
        await().atMost(1, TimeUnit.SECONDS)
                .until(() -> wsEndpoint.getMessages(), hasSize(1));
        assertThat(wsEndpoint.getMessages().get(0)).isEqualTo("someText");
    }

    private void triggerMessage(String jsonObject) {
        // @formatter:off
        given()
                .body(jsonObject)
        .when()
                .post("/my-http-source")
        .then()
                .statusCode(202);
        // @formatter:on
    }

    @AfterEach
    void cleanUp() {
        wsEndpoint.getMessages().clear();
    }

}
