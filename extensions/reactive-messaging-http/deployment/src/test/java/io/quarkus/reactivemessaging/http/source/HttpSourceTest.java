package io.quarkus.reactivemessaging.http.source;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.quarkus.reactivemessaging.http.source.app.Consumer;
import io.quarkus.test.QuarkusUnitTest;

class HttpSourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Consumer.class))
            .withConfigurationResource("http-source-test-application.properties");

    @Inject
    Consumer consumer;

    @BeforeEach
    void setUp() {
        consumer.getPostMessages().clear();
        consumer.getPutMessages().clear();
    }

    @Test
    void shouldPassTextContentAndHeaders() {
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

        List<HttpMessage> messages = consumer.getPostMessages();
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
        // @formatter:on
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
        // @formatter:off
        List<HttpMessage> messages = consumer.getPostMessages();
        assertThat(messages).hasSize(2);
    }
}
