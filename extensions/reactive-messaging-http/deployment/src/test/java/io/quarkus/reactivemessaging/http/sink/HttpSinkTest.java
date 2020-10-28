package io.quarkus.reactivemessaging.http.sink;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.http.sink.app.HttpEndpoint;
import io.quarkus.reactivemessaging.http.sink.app.HttpRepeater;
import io.quarkus.test.QuarkusUnitTest;

// mstodo non-sunny day scenario tests for everything :)
class HttpSinkTest {

    @Inject
    HttpEndpoint httpEndpoint;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(HttpRepeater.class.getPackage()))
            .withConfigurationResource("http-sink-test-application.properties");

    @AfterEach
    void cleanUp() {
        httpEndpoint.getReceivedRequests().clear();
    }

    @Test
    void shouldSendMessage() {
        // @formatter:off
        given()
                .body("some-text")
        .when()
                .post("/my-http-source")
        .then()
                .statusCode(202);
        // @formatter:on
        assertThat(httpEndpoint.getReceivedRequests()).hasSize(1);
    }
}
