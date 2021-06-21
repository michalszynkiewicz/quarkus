package io.quarkus.rest.client.reactive.dux;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.HelloClient2;
import io.quarkus.rest.client.reactive.HelloResource;
import io.quarkus.test.QuarkusUnitTest;

public class DuxIntegrationTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloClient2.class, HelloResource.class))
            .withConfigurationResource("basic-test-application.properties");

    @RestClient
    HelloClient2 client;

    @Test
    void shouldDetermineUrlViaDux() {
        String greeting = client.echo("ducks");
        assertThat(greeting).isEqualTo("hello, ducks");
    }
}
