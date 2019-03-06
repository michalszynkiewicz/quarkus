/*
 * Copyright 2019 Red Hat, Inc, and individual contributors.
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
package io.quarkus.example.restclient;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

/**
 * A resource that returns all headers as a map in response
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 2/20/19
 */
@Path("/allheaders")
@Produces(MediaType.APPLICATION_JSON)
public class AllHeadersReturningResource {

    @GET
    public Map<String, String> getAllHeaders(@Context HttpHeaders headers) {
        Map<String, String> resultMap = new HashMap<>();
        headers.getRequestHeaders().forEach(
                (key, values) -> resultMap.put(key, String.join(",", values)));
        return resultMap;
    }
}
