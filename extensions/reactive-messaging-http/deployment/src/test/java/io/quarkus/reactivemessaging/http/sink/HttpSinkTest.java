package io.quarkus.reactivemessaging.http.sink;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.http.sink.app.HttpEndpoint;
import io.quarkus.reactivemessaging.http.sink.app.HttpRepeater;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.json.JsonObject;

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

    // mstodo test header passing

    @Test
    void shouldSendMessage() {
        // @formatter:off
        given()
                .body("{\"foo\": \"bar\"}")
        .when()
                .post("/my-http-source")
        .then()
                .statusCode(202);
        // @formatter:on
        assertThat(httpEndpoint.getReceivedRequests()).hasSize(1);
        String body = httpEndpoint.getReceivedRequests().get(0).getBody();
        assertThat(new JsonObject(body)).isEqualTo(new JsonObject().put("foo", "bar"));
    }

    @Test
    void shouldUseCustomSerializer() throws InterruptedException {
        // @formatter:off
        given()
                .body("some-text")
        .when()
                .post("/custom-http-source")
        .then()
                .statusCode(202);
        // @formatter:on
        await() // mstodo why do we need to wait for it!
                .atMost(1, TimeUnit.SECONDS)
                .until(() -> httpEndpoint.getReceivedRequests(), Matchers.hasSize(1));
        String body = httpEndpoint.getReceivedRequests().get(0).getBody();
        assertThat(body).isEqualTo("SOME-TEXT");
    }
}
