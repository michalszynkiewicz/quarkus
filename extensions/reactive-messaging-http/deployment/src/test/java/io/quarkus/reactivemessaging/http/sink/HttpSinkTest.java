package io.quarkus.reactivemessaging.http.sink;

import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.http.sink.app.CustomSerializer;
import io.quarkus.reactivemessaging.http.sink.app.Dto;
import io.quarkus.reactivemessaging.http.sink.app.HttpEmitter;
import io.quarkus.reactivemessaging.http.sink.app.HttpEndpoint;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

// mstodo non-sunny day scenario tests for everything :)
class HttpSinkTest {

    @Inject
    HttpEndpoint httpEndpoint;
    @Inject
    HttpEmitter repeater;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HttpEmitter.class, CustomSerializer.class, HttpEndpoint.class, Dto.class))
            .withConfigurationResource("http-sink-test-application.properties");

    @AfterEach
    void cleanUp() {
        httpEndpoint.getReceivedRequests().clear();
    }

    // mstodo test header passing

    @Test
    void shouldSendMessage() throws InterruptedException {
        emit(Buffer.buffer("{\"foo\": \"bar\"}"));
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

    // mstodo add content-type headers from serializer?

    @Test
    void shouldSerializeCollectionToJson() throws InterruptedException {
        emit(asList(new Dto("foo"), new Dto("bar")));

        assertThat(httpEndpoint.getReceivedRequests()).hasSize(1);
        String body = httpEndpoint.getReceivedRequests().get(0).getBody();
        assertThat(new JsonArray(body)).isEqualTo(new JsonArray("[{\"field\": \"foo\"}, {\"field\": \"bar\"}]"));
    }

    @Test
    void shouldSerializeObjectToJson() throws InterruptedException {
        emit(new Dto("fooo"));

        List<HttpEndpoint.Request> requests = httpEndpoint.getReceivedRequests();
        assertThat(requests).hasSize(1);
        String body = requests.get(0).getBody();
        assertThat(new JsonObject(body)).isEqualTo(new JsonObject("{\"field\": \"fooo\"}"));
    }

    @Test
    void shouldRetry() throws InterruptedException {
        httpEndpoint.setInitialFailures(1);
        emit(repeater::retryingEmitMessage, new Dto("fooo"));

        List<HttpEndpoint.Request> requests = httpEndpoint.getReceivedRequests();
        assertThat(requests).hasSize(1);
        String body = requests.get(0).getBody();
        assertThat(new JsonObject(body)).isEqualTo(new JsonObject("{\"field\": \"fooo\"}"));
    }

    @Test
    void shouldNotRetryByDefault() throws InterruptedException {
        httpEndpoint.setInitialFailures(1);
        emit(new Dto("fooo"));
        emit(new Dto("fooo2"));

        List<HttpEndpoint.Request> requests = httpEndpoint.getReceivedRequests();
        assertThat(requests).hasSize(1);
        String body = requests.get(0).getBody();
        assertThat(new JsonObject(body)).isEqualTo(new JsonObject("{\"field\": \"fooo2\"}"));
    }

    private void emit(Object payload) throws InterruptedException {
        emit(repeater::emitMessage, payload);
    }

    private void emit(Function<Object, CompletionStage<Void>> emitter, Object payload) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        emitter.apply(payload).thenRun(done::countDown);
        done.await(1, TimeUnit.SECONDS);
    }

    // mstodo support headers, incoming and outgoing

}
