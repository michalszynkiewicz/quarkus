package io.quarkus.rest.rest.client.microprofile.recorder;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

import io.quarkus.rest.rest.client.microprofile.BuilderResolver;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RestClientRecorder {
    public void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }

    public void createRestClient(String toString, String baseUri, String configPrefix) {
    }
}
