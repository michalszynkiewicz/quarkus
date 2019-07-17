package io.quarkus.it.main;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.response.ResponseBody;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 11/07/2019
 */
public class ReactiveHttpConsumerTest {
    private static final int WAIT_TIME = 500; // [ms]

    @BeforeEach
    public void reset() {
        // @formatter:off
        when()
              .delete("/reactive-http-endpoint")
        .then()
              .statusCode(is(204));
        // @formatter:on
    }

    @Test
    public void shouldConsumeSingleMessage() throws InterruptedException {
        // @formatter:off
        given()
              .body("test-message")
        .when()
              .post("/reactive-http")
        .then()
              .statusCode(is(202));
        // @formatter:on

        waitForResponse("test-message");
    }

    private void waitForResponse(String expectedResponse) throws InterruptedException {
        long waitUntil = System.currentTimeMillis() + WAIT_TIME;
        String lastResponse = null;
        while (System.currentTimeMillis() < waitUntil) {
            Thread.sleep(50);

            ResponseBody body = when().get("reactive-http-endpoint").body();

            lastResponse = body.asString();
            if (expectedResponse.equals(lastResponse)) {
                return;
            }
        }
        fail("Failed to get the expected response: " + expectedResponse + " in time (" + WAIT_TIME + " ms). " +
                "The latest response was: " + lastResponse);
    }
}
