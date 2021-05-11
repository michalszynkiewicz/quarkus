package io.quarkus.it.rest.client.multipart;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MultipartResourceTest {

    @Test
    public void testMultipartDataIsSent() {
        // @formatter:off
        given()
                .header("Content-Type", "text/plain")
        .when().get("/client")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
        // @formatter:on
    }
}
