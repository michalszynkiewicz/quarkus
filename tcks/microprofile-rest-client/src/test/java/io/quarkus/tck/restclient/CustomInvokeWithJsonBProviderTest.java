package io.quarkus.tck.restclient;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;
import org.eclipse.microprofile.rest.client.tck.InvokeWithJsonBProviderTest;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import io.quarkus.rest.rest.client.microprofile.BuilderResolver;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.SmallRyeConfig;

/**
 *
 */
public class CustomInvokeWithJsonBProviderTest extends InvokeWithJsonBProviderTest {
    @BeforeTest
    public void setupClient() throws Exception {
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
        RestClientBuilderResolver.setInstance(new BuilderResolver());
        try {
            super.setupClient();
        } catch (IllegalArgumentException ignored) {
            // this method is run twice, once before Quarkus stuff is initialized.
            // The first execution will throw an exception
        }
    }

    @AfterTest
    public void tearDownClient() {
        ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        try {
            Config old = cpr.getConfig();
            cpr.releaseConfig(old);
        } catch (IllegalStateException ignored) {
        }
    }
}
