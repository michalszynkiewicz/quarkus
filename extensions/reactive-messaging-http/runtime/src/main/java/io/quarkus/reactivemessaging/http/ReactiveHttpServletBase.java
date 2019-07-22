package io.quarkus.reactivemessaging.http;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;
import org.reactivestreams.Processor;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         Date: 09/07/2019
 */
public abstract class ReactiveHttpServletBase<T> extends HttpServlet {

    private static final Logger log = Logger.getLogger(ReactiveHttpServletBase.class);

    private final Map<HttpMethod, Processor<HttpMessage<?>, HttpMessage<?>>> processorByMethod = new EnumMap<>(
            HttpMethod.class);
    private final Map<HttpMethod, Deserializer<?>> deserializerByMethod = new EnumMap<>(HttpMethod.class);

    public void initializeProcessor(@Observes HttpProcessorCreated event) {
        if (event.path.equals(path()) && methodMatches(event)) {
            processorByMethod.put(event.method, event.processor);
            deserializerByMethod.put(event.method, event.deserializer);
        }
    }

    private boolean methodMatches(HttpProcessorCreated event) {
        for (HttpMethod method : methods()) {
            if (method == event.method) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleRequest(HttpMethod.POST, request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleRequest(HttpMethod.PUT, request, response);
    }

    private void handleRequest(HttpMethod method, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Processor<HttpMessage<?>, HttpMessage<?>> processor = processorByMethod.get(method);
        if (processor == null) {
            respond(response, HttpResponseStatus.METHOD_NOT_ALLOWED,
                    "method " + method + " is not allowed for " + request.getServletPath() + ".");
            if (log.isDebugEnabled()) {
                log.debugf("Failed to consume a %s method call to %s. Available reactive http endpoints: %s",
                        method, request.getServletPath(), methods());
            }
            return;
        }

        HttpMessage<?> message;
        try {
            Object content = deserializerByMethod.get(method).deserialize(request);
            message = new HttpMessage<>(content);
        } catch (Exception e) {
            respond(response, HttpResponseStatus.BAD_REQUEST, e.getMessage());
            return;
        }
        processor.onNext(message);

        response.setStatus(HttpResponseStatus.ACCEPTED.code());
    }

    private void respond(HttpServletResponse response, HttpResponseStatus status, String message) throws IOException {
        response.setStatus(status.code());
        response.getWriter().println(message);
    }

    protected abstract String path();

    protected abstract HttpMethod[] methods();
}
