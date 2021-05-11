package org.jboss.resteasy.reactive.client.handlers;

import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.web.client.impl.MultipartFormUpload;
import io.vertx.ext.web.multipart.MultipartForm;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Variant;
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
        Future<HttpClientRequest> future = createRequest(requestContext);
        // DNS failures happen before we send the request
        future.onFailure(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                if (event instanceof IOException) {
                    requestContext.resume(new ProcessingException(event));
                } else {
                    requestContext.resume(event);
                }
            }
        });
        future.onSuccess(new Handler<HttpClientRequest>() {
            @Override
            public void handle(HttpClientRequest httpClientRequest) {
                Future<HttpClientResponse> sent;
                if (requestContext.isMultipart()) {
                    ReadStream<Buffer> actualEntity;
                    try {
                        actualEntity = ClientSendRequestHandler.this.setMultipartHeadersAndPrepareBody(httpClientRequest,
                                requestContext);
                        sent = httpClientRequest.send(actualEntity);
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

                sent.onSuccess(new Handler<HttpClientResponse>() {
                    @Override
                    public void handle(HttpClientResponse clientResponse) {
                        try {
                            requestContext.initialiseResponse(clientResponse);
                            if (!requestContext.isRegisterBodyHandler()) {
                                clientResponse.pause();
                                requestContext.resume();
                            } else {
                                clientResponse.bodyHandler(new Handler<Buffer>() {
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
                        .onFailure(new Handler<Throwable>() {
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

    public Future<HttpClientRequest> createRequest(RestClientRequestContext state) {
        HttpClient httpClient = state.getHttpClient();
        URI uri = state.getUri();
        boolean isHttps = "https".equals(uri.getScheme());
        int port = uri.getPort() != -1 ? uri.getPort() : (isHttps ? 443 : 80);
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setHost(uri.getHost());
        requestOptions.setPort(port);
        requestOptions.setMethod(HttpMethod.valueOf(state.getHttpMethod()));
        requestOptions.setURI(uri.getPath() + (uri.getQuery() == null ? "" : "?" + uri.getQuery()));
        requestOptions.setFollowRedirects(followRedirects);
        requestOptions.setSsl(isHttps);
        return httpClient.request(requestOptions);
    }

    private ReadStream<Buffer> setMultipartHeadersAndPrepareBody(HttpClientRequest httpClientRequest,
            RestClientRequestContext state) throws Exception {
        if (!(state.getEntity().getEntity() instanceof MultipartForm)) {
            throw new IllegalArgumentException(
                    "Multipart form upload expects an entity of type MultipartForm, got: " + state.getEntity().getEntity());
        }
        MultivaluedMap<String, String> headerMap = state.getRequestHeaders().asMap();
        MultipartForm entity = (MultipartForm) state.getEntity().getEntity();
        MultipartFormUpload multipartFormUpload = new MultipartFormUpload(Vertx.currentContext(), entity, true,
                HttpPostRequestEncoder.EncoderMode.RFC1738);// mstodo  allow modifying this
        setEntityRelatedHeaders(headerMap, state.getEntity());
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
