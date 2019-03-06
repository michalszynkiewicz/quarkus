/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.example.test;

import static io.quarkus.example.restclient.HeaderConsumingResource.HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import java.net.URI;
import java.util.Map;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 2/20/19
 */
@QuarkusTest
public class HeaderPassingTest {

    @TestHTTPResource("v1")
    URI baseUri;

    @Test
    public void shouldPassHeadersFromClientAutomatically() throws IllegalStateException, RestClientDefinitionException {
        String headerValue = "some header value";

        HeaderConsumingClient client = RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(HeaderConsumingClient.class);

        Map<String, String> headers = client.postWithHeader(headerValue, baseUri.toString());
        assertThat(headers).isNotNull();
        assertThat(headers.get(HEADER_NAME)).isEqualTo(headerValue);
    }

}
