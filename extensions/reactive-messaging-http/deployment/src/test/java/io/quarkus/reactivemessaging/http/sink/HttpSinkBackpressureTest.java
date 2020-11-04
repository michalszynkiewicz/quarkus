package io.quarkus.reactivemessaging.http.sink;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.http.sink.app.HttpEndpoint;
import io.quarkus.reactivemessaging.http.sink.app.HttpRepeater;
import io.quarkus.test.QuarkusUnitTest;

class HttpSinkBackpressureTest {

    @Inject
    HttpEndpoint httpEndpoint;
    @Inject
    HttpRepeater repeater;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(HttpRepeater.class.getPackage()))
            .withConfigurationResource("http-sink-test-application.properties");

    @Test
    void shouldApplyBackpressure() {

    }
}
