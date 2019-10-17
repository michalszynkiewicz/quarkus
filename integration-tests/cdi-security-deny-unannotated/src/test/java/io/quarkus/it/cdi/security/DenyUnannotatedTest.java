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
 */
@QuarkusTest
public class DenyUnannotatedTest {

    @Test
    public void shouldDenyUnannotated() {
        assertForAnonymous("/api/unannotatedMethod", 401, Optional.empty());
        assertForUsers("/api/unannotatedMethod", 403, Optional.empty());
    }

    @Test
    public void shouldAllowPermitAll() {
        assertForAnonymous("/api/allowedMethod", 200, Optional.of("allowed"));
        assertForUsers("/api/allowedMethod", 200, Optional.of("allowed"));
    }

    @Test
    public void shouldRestrict() {
        String path = "/api/restrictedMethod";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 403, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), path, 200,
                Optional.of("accessibleForAdminOnly"));
    }

    @Test
    public void shouldNotInheritPermitAll() {
        assertForAnonymous("/api/unannotatedInPermitAllSubclass", 401, Optional.empty());
        assertForUsers("/api/unannotatedInPermitAllSubclass", 403, Optional.empty());
    }

    @Test
    public void shouldAllowUnannotatedOnBeanWithNoSecurityAnnotations() {
        String path = "api/unannotatedOnBeanWithNoAnno";
        assertForAnonymous(path, 200, Optional.of("unannotatedOnBeanWithNoAnno"));
        assertForUsers(path, 200, Optional.of("unannotatedOnBeanWithNoAnno"));
    }

    @Test
    public void shouldDenyMethodInherittedFromBeanDeniedByDefault() {
        String path = "api/inheritedDeniedByDefault";
        assertForAnonymous(path, 401, Optional.empty());
        assertForUsers(path, 403, Optional.empty());
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
