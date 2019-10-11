package io.quarkus.it.cdi.security;

import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 04/10/2019
 */
@QuarkusTest
public class CDIAccessTest {

    private static final String ALICE = "alice";
    private static final String ADMIN = "admin";

    @Test
    public void shouldFailToAccessForbidden() {
        assertForAnonymous("/api/forbiddenMethod", 401, Optional.empty());
        assertForUsers("/api/forbiddenMethod", 403, Optional.empty());
    }

    @Test
    public void shouldAccessAllowed() {
        assertForAnonymous("/api/allowedMethod", 200, Optional.of("accessibleForAll"));
        assertForUsers("/api/allowedMethod", 200, Optional.of("accessibleForAll"));
    }

    @Test
    public void shouldRestrictAccessToSpecificRole() {
        String path = "/api/securedMethod";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 403, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), path, 200,
                Optional.of("accessibleForAdminOnly"));
    }

    @Test
    public void shouldFailToAccessForbiddenOnClass() {
        assertForAnonymous("/api/forbiddenOnClass", 401, Optional.empty());
        assertForUsers("/api/forbiddenOnClass", 403, Optional.empty());
    }

    @Test
    public void shouldAccessAllowedMethodOnForbiddenClass() {
        assertForUsers("/api/allowedMethodOnForbiddenClass", 200, Optional.of("allowedOnMethod"));
    }

    @Test
    public void shouldRestrictAccessToRoleOnMethod() {
        String path = "/api/securedMethodOnForbiddenClass";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 403, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), path, 200,
                Optional.of("restrictedOnMethod"));
    }

    @Test
    public void shouldAccessInherittedAllowedMethod() {
        assertForAnonymous("/api/allowedMethodOnForbiddenClass", 200, Optional.of("allowedOnMethod"));
        assertForUsers("/api/allowedMethodOnForbiddenClass", 200, Optional.of("allowedOnMethod"));
    }

    @Test
    public void shouldFailToAccessForbiddenInheritedMethod() {
        assertForAnonymous("/api/inheritedForbiddenMethod", 401, Optional.empty());
        assertForUsers("/api/inheritedForbiddenMethod", 403, Optional.empty());
    }

    @Test
    public void shouldAccessAllowedOnClass() {
        assertForUsers("/api/allowedOnClass", 200, Optional.of("allowedOnClass"));
    }

    @Test
    public void shouldFailToAccessForbiddenMethodOfPermitAllClass() {
        assertForAnonymous("/api/forbiddenMethodOnFreeAccessClass", 401, Optional.empty());
        assertForUsers("/api/forbiddenMethodOnFreeAccessClass", 403, Optional.empty());
    }

    @Test
    public void shouldRestrictAccessForRestrictedMethodOfPermitAllClass() {
        String path = "/api/restrictedMethodOnFreeAccessClass";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 403, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), path, 200,
                Optional.of("restrictedOnMethod"));
    }

    @Test
    public void shouldAccessInheritedAllowedOnPermitAllClass() {
        assertForUsers("/api/inheritedAllowedMethodOnPermitAllClass", 200, Optional.of("accessibleForAll"));
    }

    @Test
    public void shouldFailToAccessInheritedForbiddenOnPermitAllClass() {
        assertForAnonymous("/api/inheritedForbiddenMethodOnPermitAllClass", 401, Optional.empty());
        assertForUsers("/api/inheritedForbiddenMethodOnPermitAllClass", 403, Optional.empty());
    }

    @Test
    public void shouldFailToAccessInheritedForbiddenOnUnannotatedClass() {
        assertForAnonymous("/api/inheritedForbiddenOnUnannotatedClass", 401, Optional.empty());
        assertForUsers("/api/inheritedForbiddenOnUnannotatedClass", 403, Optional.empty());
    }

    private void assertForAnonymous(String path, int status, Optional<String> content) {
        assertStatusAndContent(RestAssured.given(), path, status, content);
    }

    private void assertForUsers(String path, int status, Optional<String> content) {
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, status, content);
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), path, status, content);
    }

    private void assertStatusAndContent(RequestSpecification request, String path, int status, Optional<String> content) {
        ValidatableResponse validatableResponse = request.when().get(path)
                .then()
                .statusCode(status);
        content.ifPresent(text -> validatableResponse.body(Matchers.equalTo(text)));
    }
}
