package io.quarkus.reactivemessaging.http;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.jboss.logging.Logger;
import org.reactivestreams.Processor;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         Date: 09/07/2019
 */
@WebServlet
public class ReactiveHttpServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(ReactiveHttpServlet.class);

    @Connector(QuarkusHttpConnector.NAME)
    QuarkusHttpConnector connector;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleRequest(HttpMethod.POST.name(), request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleRequest(HttpMethod.PUT.name(), request, response);
    }

    private void handleRequest(String method, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // mstodo: cache the processor or create the processor before creating the servlet.
        // mstodo you know which processors are needed upfront
        // mstodo: drawback: some unneeded processor (without consumer) can be created??
        Processor<HttpMessage<?>, HttpMessage<?>> processor = connector
                .getProcessor(new HttpInputDescriptor(request.getServletPath(), method));
        if (processor == null) {
            respond(response, HttpResponseStatus.METHOD_NOT_ALLOWED,
                    "method " + method + " is not allowed for " + request.getServletPath() + ".");
            if (log.isDebugEnabled()) {
                log.debugf("Failed to consume a %s method call to %s. Available reactive http endpoints: %s",
                        method, request.getServletPath(), connector.getProcessors());
            }
            return;
        }

        HttpMessage<?> message = null;
        try {
            message = new HttpMessage<>(request); // mstodo deserializing!!!!!
        } catch (PayloadParsingException e) {
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
}
