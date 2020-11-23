package io.quarkus.reactivemessaging.http.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata for messages sent out by the http connector
 */
public class OutgoingHttpMetadata {
    private final Map<String, List<String>> query;
    private final Map<String, List<String>> headers;
    private final Map<String, String> pathParameters;

    private OutgoingHttpMetadata(Map<String, String> pathParameters, Map<String, List<String>> query,
            Map<String, List<String>> headers) {
        this.pathParameters = pathParameters;
        this.query = query;
        this.headers = headers;
    }

    Map<String, List<String>> getHeaders() {
        return headers;
    }

    Map<String, List<String>> getQuery() {
        return query;
    }

    Map<String, String> getPathParameters() {
        return pathParameters;
    }

    public static final class Builder {
        private Map<String, List<String>> query;
        private Map<String, List<String>> headers;
        private Map<String, String> pathParameters;

        public Builder addQueryParameter(String paramName, String paramValue) {
            if (query == null) {
                query = new HashMap<>();
            }
            query.computeIfAbsent(paramName, whatever -> new ArrayList<>())
                    .add(paramValue);
            return this;
        }

        public Builder addHeader(String headerName, String headerValue) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            headers.computeIfAbsent(headerName, whatever -> new ArrayList<>())
                    .add(headerValue);
            return this;
        }

        public Builder addPathParameter(String parameter, String value) {
            if (pathParameters == null) {
                pathParameters = new HashMap<>();
            }
            pathParameters.put(parameter, value);
            return this;
        }

        public OutgoingHttpMetadata build() {
            return new OutgoingHttpMetadata(
                    pathParameters == null ? Collections.emptyMap() : pathParameters,
                    query == null ? Collections.emptyMap() : query,
                    headers == null ? Collections.emptyMap() : headers);
        }
    }

    // mstodo javadoc

}
