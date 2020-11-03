package io.quarkus.reactivemessaging.http.source;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.quarkus.reactivemessaging.http.source.app.Consumer;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class HttpSourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Consumer.class))
            .withConfigurationResource("http-source-test-application.properties");

    @Inject
    Consumer consumer;

    @AfterEach
    void setUp() {
        consumer.getPostMessages().clear();
        consumer.getPutMessages().clear();
        consumer.getPayloads().clear();
    }

    @Test
    void shouldPassTextContentAndHeaders() {
        // mstodo proper header support
        String headerName = "my-custom-header";
        String headerValue = "my-custom-header-value";
        // @formatter:off
        given()
                .header(headerName, headerValue)
                .body("some-text")
        .when()
                .post("/my-http-source")
        .then()
                .statusCode(202);
        // @formatter:on

        List<HttpMessage<?>> messages = consumer.getPostMessages();
        assertThat(messages).hasSize(1);
        HttpMessage message = messages.get(0);
        assertThat(message.getPayload().toString()).isEqualTo("some-text");
        assertThat(message.getHttpHeaders().get(headerName)).isEqualTo(headerValue);
    }

    @Test
    void shouldDifferentiatePostAndPut() {
        // @formatter:off
        given()
                .body("some-text")
        .when()
                .put("/my-http-source")
        .then()
                .statusCode(202);

        given()
                .body("some-text")
        .when()
                .post("/my-http-source")
        .then()
                .statusCode(202);
        // @formatter:on
        assertThat(consumer.getPostMessages()).hasSize(1);
        assertThat(consumer.getPutMessages()).hasSize(1);
    }

    @Test
    void shouldConsumeHttpTwice() {
        // @formatter:off
        given()
                .body("some-text")
        .when()
                .post("/my-http-source")
        .then()
                .statusCode(202);

        given()
                .body("some-text")
        .when()
                .post("/my-http-source")
        .then()
                .statusCode(202);
        // @formatter:on
        List<HttpMessage<?>> messages = consumer.getPostMessages();
        assertThat(messages).hasSize(2);
    }

    @Test
    void shouldConsumeJsonObject() {
        // @formatter:off
        given()
                .body("{\"some\": \"json\"}")
        .when()
                .post("/json-http-source")
        .then()
                .statusCode(202);
        // @formatter:on

        List<?> payloads = consumer.getPayloads();
        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0)).isInstanceOf(JsonObject.class);
        JsonObject payload = (JsonObject) payloads.get(0);
        assertThat(payload.getString("some")).isEqualTo("json");
    }

    @Test
    void shouldConsumeJsonArray() {
        // mstodo proper header support
        // @formatter:off
        given()
                .body("[{\"some\": \"json\"}]")
        .when()
                .post("/jsonarray-http-source")
        .then()
                .statusCode(202);
        // @formatter:on

        List<?> payloads = consumer.getPayloads();
        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0)).isInstanceOf(JsonArray.class);
        JsonArray payload = (JsonArray) payloads.get(0);
        assertThat(payload.getJsonObject(0).getString("some")).isEqualTo("json");
    }

    @Test
    void shouldConsumeString() {
        // @formatter:off
        given()
                .body("someString")
        .when()
                .post("/string-http-source")
        .then()
                .statusCode(202);
        // @formatter:on

        List<?> payloads = consumer.getPayloads();
        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0)).isInstanceOf(String.class);
        String payload = (String) payloads.get(0);
        assertThat(payload).isEqualTo("someString");
    }
}
