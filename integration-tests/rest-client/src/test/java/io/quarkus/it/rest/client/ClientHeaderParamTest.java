package io.quarkus.it.rest.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ClientHeaderParamTest {

    @Test
    void testDefaultMethod() {
        given()
                .header("Content-Type", "text/plain")
                .when().get("/header")
                .then()
                .statusCode(200)
                .body(equalTo(HeaderPassingClient.COMPUTED_VALUE + "," + HeaderPassingClient.STATIC_COMPUTED_VALUE));
    }
}
