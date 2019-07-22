package io.quarkus.reactivemessaging.http;

import org.reactivestreams.Processor;

/**
 * mstodo: one class for both created events?
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 19/07/2019
 */
public class HttpProcessorCreated {
    public final String path;
    public final HttpMethod method;
    public final Processor<HttpMessage<?>, HttpMessage<?>> processor;
    public final Deserializer<?> deserializer;

    public HttpProcessorCreated(String path,
            HttpMethod method,
            Processor<HttpMessage<?>, HttpMessage<?>> processor,
            Deserializer<?> deserializer) {
        this.path = path;
        this.processor = processor;
        this.method = method;
        this.deserializer = deserializer;
    }

    @Override
    public String toString() {
        return "HttpProcessorCreated{" +
                "path='" + path + '\'' +
                ", processor=" + processor +
                '}';
    }
}
