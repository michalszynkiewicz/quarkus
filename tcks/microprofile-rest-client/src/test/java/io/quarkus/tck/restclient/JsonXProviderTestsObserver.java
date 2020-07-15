package io.quarkus.tck.restclient;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.Before;

import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.SmallRyeConfig;

public class JsonXProviderTestsObserver {

    private static final Set<String> APPROPRIATE_TESTS = new HashSet<>(
            asList("InvokeWithJsonBProviderTest", "InvokeWithJsonBProviderTest"));

    public void before(@Observes(precedence = 1000) Before event) {
        if (!APPROPRIATE_TESTS.contains(event.getTestClass().getJavaClass().getSimpleName())) {
            return;
        }
        SmallRyeConfig config = ConfigUtils.configBuilder(true).build();
        QuarkusConfigFactory.setConfig(config);
        ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        try {
            Config old = cpr.getConfig();
            if (old != config) {
                cpr.releaseConfig(old);
            }
        } catch (IllegalStateException ignored) {
        }
    }

    public void after(@Observes(precedence = 1000) After event) {
        if (!APPROPRIATE_TESTS.contains(event.getTestClass().getJavaClass().getSimpleName())) {
            return;
        }
        ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        try {
            Config old = cpr.getConfig();
            cpr.releaseConfig(old);
        } catch (IllegalStateException ignored) {
        }
    }
}
