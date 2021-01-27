package org.jboss.resteasy.reactive.client.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.spi.ClientContextResolver;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;

import io.vertx.core.buffer.Buffer;

public class ClientBuilderImpl extends ClientBuilder {

    private static final Logger log = Logger.getLogger(ClientBuilderImpl.class);

    private static final ClientContextResolver CLIENT_CONTEXT_RESOLVER = ClientContextResolver.getInstance();
    private static final char[] storePassword = randomAlphanumeric(10);

    private ClientProxies clientProxies;
    private ConfigurationImpl configuration;
    private SSLContext sslContext;
    private KeyStore trustStore;
    private KeyStore keyStore;
    private char[] keystorePassword;
    private HostnameVerifier hostnameVerifier;
    private ExecutorService executorService;

    @Override
    public ClientBuilder withConfig(Configuration config) {
        this.configuration = new ConfigurationImpl(config);
        return this;
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        this.keyStore = null;
        this.trustStore = null;
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, char[] password) {
        this.keyStore = keyStore;
        this.keystorePassword = password;
        return this;
    }

    @Override
    public ClientBuilder trustStore(KeyStore trustStore) {
        this.trustStore = trustStore;
        return this;
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        this.hostnameVerifier = verifier;
        return this;
    }

    @Override
    public ClientBuilder executorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    @Override
    public ClientBuilder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        return this;
    }

    @Override
    public ClientBuilder connectTimeout(long timeout, TimeUnit unit) {
        // TODO
        return this;
    }

    @Override
    public ClientBuilder readTimeout(long timeout, TimeUnit unit) {
        // TODO
        return this;
    }

    @Override
    public ClientImpl build() {

        Buffer keyStore = asBuffer(this.keyStore);
        Buffer trustStore = asBuffer(this.trustStore);

        // mstodo ssl context!!!

        return new ClientImpl(configuration,
                CLIENT_CONTEXT_RESOLVER.resolve(Thread.currentThread().getContextClassLoader()),
                hostnameVerifier,
                keystorePassword == null ? null : new String(keystorePassword),
                keyStore,
                trustStore,
                sslContext,
                executorService);

    }

    private Buffer asBuffer(KeyStore keyStore) {
        if (keyStore != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                keyStore.store(out, storePassword);
                return Buffer.buffer(out.toByteArray());
            } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
                log.error("Failed to translate keystore to vert.x keystore", e);
            }
        }
        return null;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public ClientBuilder property(String name, Object value) {
        configuration.property(name, value);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Class<?> componentClass) {
        configuration.register(componentClass);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Class<?> componentClass, int priority) {
        configuration.register(componentClass, priority);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Class<?> componentClass, Class<?>... contracts) {
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Object component) {
        configuration.register(component);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Object component, int priority) {
        configuration.register(component, priority);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Object component, Class<?>... contracts) {
        configuration.register(component, contracts);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Object component, Map<Class<?>, Integer> contracts) {
        configuration.register(component, contracts);
        return this;
    }

    // mstodo test
    private static char[] randomAlphanumeric(int length) {
        Random random = new Random();
        char[] password = new char[length];

        for (int i = 0; i < length; i++) {
            int randomNumber = random.nextInt(60);

            if (randomNumber < 10) {
                password[i] = (char) (randomNumber + '0');
            } else if (randomNumber < (10 + 25)) {
                password[i] = (char) (randomNumber - 10 + 'a');
            } else {
                password[i] = (char) (randomNumber - 10 - 25 + 'A');
            }
        }
        return password;
    }
}
