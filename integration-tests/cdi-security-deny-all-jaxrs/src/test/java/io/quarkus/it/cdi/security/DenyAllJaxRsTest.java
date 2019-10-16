package io.quarkus.it.cdi.security;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 11/10/2019
 */
@QuarkusTest
public class DenyAllJaxRsTest {

    @Test
    public void shouldDenyUnannotated() {
        String path = "/unsecured/defaultSecurity";
        assertForAnonymous(path, 401);
        assertForUser(path, 403);
    }

    @Test
    public void shouldDenyDenyAllMethod() {
        String path = "/unsecured/denyAll";
        assertForAnonymous(path, 401);
        assertForUser(path, 403);
    }

    @Test
    public void shouldPermitPermitAllMethod() {
        String path = "/unsecured/permitAll";
        assertForAnonymous(path, 200);
        assertForUser(path, 200);
    }

    @Test
    public void shouldDenySubResource() {
        String path = "/unsecured/sub/subMethod";
        assertForAnonymous(path, 401);
        assertForUser(path, 403);
    }

    @Test
    public void shouldAllowPermitAllSubResource() {
        String path = "/unsecured/permitAllSub/subMethod";
        assertForAnonymous(path, 200);
        assertForUser(path, 200);
    }

    @Test
    public void shouldAllowPermitAllClass() {
        String path = "/permitAll/sub/subMethod";
        assertForAnonymous(path, 200);
        assertForUser(path, 200);
    }

    private void assertForAnonymous(String path, int status) {
        assertStatusAndContent(RestAssured.given(), path, status);
    }

    private void assertForUser(String path, int status) {
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, status);
    }

    private void assertStatusAndContent(RequestSpecification request, String path, int status) {
        request.when().get(path)
                .then()
                .statusCode(status);
    }

}
