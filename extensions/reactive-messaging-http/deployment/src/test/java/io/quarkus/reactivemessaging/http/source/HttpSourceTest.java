package io.quarkus.reactivemessaging.http.source;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.http.runtime.HttpMessage;
import io.quarkus.reactivemessaging.http.source.app.Consumer;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.ValidatableResponse;
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
        consumer.clear();
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
        HttpMessage<?> message = messages.get(0);
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
        // @formatter:on

        send("some-text", "/my-http-source");
        assertThat(consumer.getPostMessages()).hasSize(1);
        assertThat(consumer.getPutMessages()).hasSize(1);
    }

    @Test
    void shouldConsumeHttpTwice() {
        send("some-text", "/my-http-source");

        send("some-text", "/my-http-source");
        List<HttpMessage<?>> messages = consumer.getPostMessages();
        assertThat(messages).hasSize(2);
    }

    @Test
    void shouldConsumeJsonObject() {
        send("{\"some\": \"json\"}", "/json-http-source");

        List<?> payloads = consumer.getPayloads();
        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0)).isInstanceOf(JsonObject.class);
        JsonObject payload = (JsonObject) payloads.get(0);
        assertThat(payload.getString("some")).isEqualTo("json");
    }

    @Test
    void shouldConsumeJsonArray() {
        send("[{\"some\": \"json\"}]", "/jsonarray-http-source");

        List<?> payloads = consumer.getPayloads();
        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0)).isInstanceOf(JsonArray.class);
        JsonArray payload = (JsonArray) payloads.get(0);
        assertThat(payload.getJsonObject(0).getString("some")).isEqualTo("json");
    }

    @Test
    void shouldConsumeString() {
        send("someString", "/string-http-source");

        List<?> payloads = consumer.getPayloads();
        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0)).isInstanceOf(String.class);
        String payload = (String) payloads.get(0);
        assertThat(payload).isEqualTo("someString");
    }

    @Test
    void shouldBuffer13MessagesIfConfigured() throws InterruptedException {
        // 1 message should start being consumed, 13 should be buffered, the rest should respond with 503
        consumer.pause();
        List<Future<Integer>> sendStates = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(17);
        for (int i = 0; i < 17; i++) {
            sendStates.add(executorService.submit(() -> sendAndGetStatus("some-text", "/my-http-source")));
        }
        System.out.println("initiated send for all messages waiting for failures");

        //         await a 503
        // mstodo weird but it fails
        await("assert 3 failures")
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> countCodes(sendStates, 503), Predicate.isEqual(3));
        //
        //        Thread.sleep(4000L); // mstodo remove

        System.out.println("wait for failures done");
        consumer.resume();
        System.out.println("consumer resume called");

        // mstodo remove
        Thread.sleep(20000L); // mstodo remove
        System.out.println("after 20s sleep");
        System.out.flush();

        int failures = 0;

        System.out.printf("202s: %d\n", countCodes(sendStates, 202));
        System.out.printf("503: %d\n", countCodes(sendStates, 503));

        //        System.out.printf("Successes: %d, failures: %d", successes, 17 - successes);

        // mstodo end
        assertThat(consumer.getPostMessages()).hasSize(14);
    }

    private long countCodes(List<Future<Integer>> sendStates, int code) {
        List<Integer> statusCodes = new ArrayList<>();
        for (Future<Integer> sendState : sendStates) {
            if (sendState.isDone()) {
                try {
                    statusCodes.add(sendState.get());
                } catch (InterruptedException | ExecutionException e) {
                    fail("checking the status code for http connection failed unexpectedly", e);
                }
            } else {
                System.out.println("not done");
            }
        }

        return statusCodes.stream().filter(it -> it == code).count();
    }

    static int sendAndGetStatus(String body, String path) {
        return sendAndGetResponse(body, path).extract().statusCode();
    }

    static ValidatableResponse sendAndGetResponse(String body, String path) {
        return given().body(body)
                .when().post(path)
                .then();
    }

    static void send(String body, String path) {
        sendAndGetResponse(body, path).statusCode(202);
    }
}
