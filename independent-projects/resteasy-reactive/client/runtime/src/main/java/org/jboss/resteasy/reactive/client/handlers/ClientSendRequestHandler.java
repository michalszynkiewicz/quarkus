package org.jboss.resteasy.reactive.client.handlers;

import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.smallrye.dux.Dux;
import io.smallrye.dux.ServiceInstance;
import io.smallrye.dux.utils.DuxAddressUtils;
import io.smallrye.dux.utils.HostAndPort;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.streams.Pipe;
import io.vertx.ext.web.client.impl.MultipartFormUpload;
import io.vertx.ext.web.multipart.MultipartForm;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Variant;

import org.jboss.resteasy.reactive.client.AsyncResultUni;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.jboss.resteasy.reactive.client.impl.AsyncInvokerImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.core.Serialisers;

public class ClientSendRequestHandler implements ClientRestHandler {
    private final boolean followRedirects;

    public ClientSendRequestHandler(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    @Override
    public void handle(RestClientRequestContext requestContext) {
        if (requestContext.isAborted()) {
            return;
        }
        requestContext.suspend();
        Uni<HttpClientRequest> future = createRequest(requestContext);
        // DNS failures happen before we send the request
        future.onFailure().invoke(new Consumer<>() {
            @Override
            public void accept(Throwable event) {
                if (event instanceof IOException) {
                    requestContext.resume(new ProcessingException(event));
                } else {
                    requestContext.resume(event);
                }
            }
        });
        future.subscribe().with(new Consumer<>() {
            @Override
            public void accept(HttpClientRequest httpClientRequest) {
                Future<HttpClientResponse> sent;
                if (requestContext.isMultipart()) {
                    Promise<HttpClientRequest> requestPromise = Promise.promise();
                    MultipartFormUpload actualEntity;
                    try {
                        actualEntity = ClientSendRequestHandler.this.setMultipartHeadersAndPrepareBody(httpClientRequest,
                                requestContext);

                        Pipe<Buffer> pipe = actualEntity.pipe(); // Shouldn't this be called in an earlier phase ?
                        requestPromise.future().onComplete(ar -> {
                            if (ar.succeeded()) {
                                HttpClientRequest req = ar.result();
                                if (httpClientRequest.headers() == null
                                        || !httpClientRequest.headers().contains(HttpHeaders.CONTENT_LENGTH)) {
                                    req.setChunked(true);
                                }
                                pipe.endOnFailure(false);
                                pipe.to(req, ar2 -> {
                                    if (ar2.failed()) {
                                        req.reset(0L, ar2.cause());
                                    }
                                });
                                actualEntity.run();
                            } else {
                                pipe.close();
                            }
                        });
                        sent = httpClientRequest.response();

                        requestPromise.complete(httpClientRequest);
                    } catch (Throwable e) {
                        requestContext.resume(e);
                        return;
                    }
                } else {
                    Buffer actualEntity;
                    try {
                        actualEntity = ClientSendRequestHandler.this
                                .setRequestHeadersAndPrepareBody(httpClientRequest, requestContext);
                    } catch (Throwable e) {
                        requestContext.resume(e);
                        return;
                    }
                    if (actualEntity == AsyncInvokerImpl.EMPTY_BUFFER) {
                        sent = httpClientRequest.send();
                    } else {
                        sent = httpClientRequest.send(actualEntity);
                    }
                }

                sent.onSuccess(new Handler<>() {
                    @Override
                    public void handle(HttpClientResponse clientResponse) {
                        try {
                            requestContext.initialiseResponse(clientResponse);
                            if (!requestContext.isRegisterBodyHandler()) {
                                clientResponse.pause();
                                requestContext.resume();
                            } else {
                                clientResponse.bodyHandler(new Handler<>() {
                                    @Override
                                    public void handle(Buffer buffer) {
                                        try {
                                            if (buffer.length() > 0) {
                                                requestContext.setResponseEntityStream(
                                                        new ByteArrayInputStream(buffer.getBytes()));
                                            } else {
                                                requestContext.setResponseEntityStream(null);
                                            }
                                            requestContext.resume();
                                        } catch (Throwable t) {
                                            requestContext.resume(t);
                                        }
                                    }
                                });
                            }
                        } catch (Throwable t) {
                            requestContext.resume(t);
                        }
                    }
                })
                        .onFailure(new Handler<>() {
                            @Override
                            public void handle(Throwable failure) {
                                if (failure instanceof IOException) {
                                    requestContext.resume(new ProcessingException(failure));
                                } else {
                                    requestContext.resume(failure);
                                }
                            }
                        });
            }
        });
    }

    public Uni<HttpClientRequest> createRequest(RestClientRequestContext state) {
        HttpClient httpClient = state.getHttpClient();
        URI uri = state.getUri();

        Uni<RequestOptions> requestOptions;
        if (uri.getScheme().startsWith("dux")) {
            boolean isHttps = "duxs".equals(uri.getScheme());
            String serviceName = uri.getHost();
            Uni<ServiceInstance> serviceInstance = Dux.getInstance().getLoadBalancer(serviceName)
                    .selectServiceInstance();
            requestOptions = serviceInstance.map(ServiceInstance::getValue)
                    .map(new Function<>() {
                        @Override
                        public RequestOptions apply(String host) {
                            return configureHostAndPort(isHttps, new RequestOptions(), host, serviceName);
                        }
                    });
        } else {
            boolean isHttps = "https".equals(uri.getScheme());
            int port = getPort(isHttps, uri.getPort());
            requestOptions = Uni.createFrom().item(new RequestOptions().setHost(uri.getHost())
                    .setPort(port).setSsl(isHttps));
        }

        return requestOptions.onItem()
                .transform(r -> r.setMethod(HttpMethod.valueOf(state.getHttpMethod()))
                        .setURI(uri.getPath() + (uri.getQuery() == null ? "" : "?" + uri.getQuery()))
                        .setFollowRedirects(followRedirects))
                .flatMap(options -> AsyncResultUni.toUni(handler -> httpClient.request(options, handler)));
    }

    private RequestOptions configureHostAndPort(boolean isHttps, RequestOptions options,
            String serviceAddress, String serviceName) {
        HostAndPort hostAndPort = DuxAddressUtils.parseToHostAndPort(serviceAddress, defaultPort(isHttps), serviceName);
        options.setHost(hostAndPort.host);
        options.setPort(hostAndPort.port);
        return options;
    }

    private int getPort(boolean isHttps, int specifiedPort) {
        return specifiedPort != -1 ? specifiedPort : defaultPort(isHttps);
    }

    private int defaultPort(boolean isHttps) {
        return isHttps ? 443 : 80;
    }

    private MultipartFormUpload setMultipartHeadersAndPrepareBody(HttpClientRequest httpClientRequest,
            RestClientRequestContext state) throws Exception {
        if (!(state.getEntity().getEntity() instanceof MultipartForm)) {
            throw new IllegalArgumentException(
                    "Multipart form upload expects an entity of type MultipartForm, got: " + state.getEntity().getEntity());
        }
        MultivaluedMap<String, String> headerMap = state.getRequestHeaders().asMap();
        MultipartForm entity = (MultipartForm) state.getEntity().getEntity();
        Object property = state.getConfiguration().getProperty(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE);
        HttpPostRequestEncoder.EncoderMode mode = HttpPostRequestEncoder.EncoderMode.RFC1738;
        if (property != null) {
            mode = (HttpPostRequestEncoder.EncoderMode) property;
        }
        MultipartFormUpload multipartFormUpload = new MultipartFormUpload(Vertx.currentContext(), entity, true, mode);
        setEntityRelatedHeaders(headerMap, state.getEntity());

        // multipart has its own headers:
        MultiMap multipartHeaders = multipartFormUpload.headers();
        for (String multipartHeader : multipartHeaders.names()) {
            headerMap.put(multipartHeader, multipartHeaders.getAll(multipartHeader));
        }

        setVertxHeaders(httpClientRequest, headerMap);
        return multipartFormUpload;
    }

    private Buffer setRequestHeadersAndPrepareBody(HttpClientRequest httpClientRequest,
            RestClientRequestContext state)
            throws IOException {
        MultivaluedMap<String, String> headerMap = state.getRequestHeaders().asMap();
        Buffer actualEntity = AsyncInvokerImpl.EMPTY_BUFFER;
        Entity<?> entity = state.getEntity();
        if (entity != null) {
            // no need to set the entity.getMediaType, it comes from the variant
            setEntityRelatedHeaders(headerMap, entity);

            actualEntity = state.writeEntity(entity, headerMap,
                    state.getConfiguration().getWriterInterceptors().toArray(Serialisers.NO_WRITER_INTERCEPTOR));
        }
        // set the Vertx headers after we've run the interceptors because they can modify them
        setVertxHeaders(httpClientRequest, headerMap);
        return actualEntity;
    }

    private void setVertxHeaders(HttpClientRequest httpClientRequest, MultivaluedMap<String, String> headerMap) {
        MultiMap vertxHttpHeaders = httpClientRequest.headers();
        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            vertxHttpHeaders.add(entry.getKey(), entry.getValue());
        }
    }

    private void setEntityRelatedHeaders(MultivaluedMap<String, String> headerMap, Entity<?> entity) {
        if (entity.getVariant() != null) {
            Variant v = entity.getVariant();
            headerMap.putSingle(HttpHeaders.CONTENT_TYPE, v.getMediaType().toString());
            if (v.getLanguageString() != null) {
                headerMap.putSingle(HttpHeaders.CONTENT_LANGUAGE, v.getLanguageString());
            }
            if (v.getEncoding() != null) {
                headerMap.putSingle(HttpHeaders.CONTENT_ENCODING, v.getEncoding());
            }
        }
    }
}
