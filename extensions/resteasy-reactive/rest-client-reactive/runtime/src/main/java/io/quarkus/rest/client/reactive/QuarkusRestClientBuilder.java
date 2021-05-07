package io.quarkus.rest.client.reactive;

import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;

import io.smallrye.loadbalancer.LoadBalancer;
// mstodo does this make sense?
public interface QuarkusRestClientBuilder extends RestClientBuilder {
    @Override
    QuarkusRestClientBuilder baseUrl(URL url);

    @Override
    default QuarkusRestClientBuilder baseUri(URI uri) {
        return (QuarkusRestClientBuilder) RestClientBuilder.super.baseUri(uri);
    }

    QuarkusRestClientBuilder loadBalancer(LoadBalancer loadBalancer);

    @Override
    QuarkusRestClientBuilder connectTimeout(long timeout, TimeUnit unit);

    @Override
    QuarkusRestClientBuilder readTimeout(long timeout, TimeUnit unit);

    @Override
    QuarkusRestClientBuilder executorService(ExecutorService executor);

    @Override
    QuarkusRestClientBuilder sslContext(SSLContext sslContext);

    @Override
    QuarkusRestClientBuilder trustStore(KeyStore trustStore);

    @Override
    QuarkusRestClientBuilder keyStore(KeyStore keyStore, String keystorePassword);

    @Override
    QuarkusRestClientBuilder hostnameVerifier(HostnameVerifier hostnameVerifier);

    @Override
    QuarkusRestClientBuilder followRedirects(boolean follow);

    @Override
    QuarkusRestClientBuilder proxyAddress(String proxyHost, int proxyPort);

    @Override
    QuarkusRestClientBuilder queryParamStyle(QueryParamStyle style);

    @Override
    QuarkusRestClientBuilder property(String name, Object value);

    @Override
    QuarkusRestClientBuilder register(Class<?> componentClass);

    @Override
    QuarkusRestClientBuilder register(Class<?> componentClass, int priority);

    @Override
    QuarkusRestClientBuilder register(Class<?> componentClass, Class<?>... contracts);

    @Override
    QuarkusRestClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts);

    @Override
    QuarkusRestClientBuilder register(Object component);

    @Override
    QuarkusRestClientBuilder register(Object component, int priority);

    @Override
    QuarkusRestClientBuilder register(Object component, Class<?>... contracts);

    @Override
    QuarkusRestClientBuilder register(Object component, Map<Class<?>, Integer> contracts);
}
