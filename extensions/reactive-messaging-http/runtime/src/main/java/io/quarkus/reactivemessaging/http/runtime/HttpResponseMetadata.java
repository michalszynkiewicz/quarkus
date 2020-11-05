package io.quarkus.reactivemessaging.http.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.MultiMap;

public class HttpResponseMetadata {
    private Map<String, List<String>> query = new HashMap<>();
    private MultiMap headers = MultiMap.caseInsensitiveMultiMap();

    MultiMap getHeaders() {
        return headers;
    }

    Map<String, List<String>> getQuery() {
        return query;
    }

    // mstodo javadoc
    public void addQueryParameter(String paramName, String paramValue) {
        query.computeIfAbsent(paramName, whatever -> new ArrayList<>())
                .add(paramValue);
    }

    public void addHeader(String headerName, String headerValue) {
        headers.add(headerName, headerValue);
    }
}
