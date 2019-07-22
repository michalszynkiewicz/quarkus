package io.quarkus.reactivemessaging.http;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.reactivemessaging.http.ReactiveHttpConnectorUtils.configKey;
import static io.quarkus.reactivemessaging.http.ReactiveHttpConnectorUtils.connectorNames;
import static io.quarkus.reactivemessaging.http.ReactiveHttpConnectorUtils.toUniqueClassName;

import java.util.Random;
import java.util.Set;

import javax.inject.Inject;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.undertow.websockets.deployment.AnnotatedWebsocketEndpointBuildItem;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 17/07/2019
 */
class ReactiveWebsocketProcessor {

    private static final Random random = new Random();

    @Inject
    BuildProducer<GeneratedBeanBuildItem> generatedBean;

    @Inject
    BuildProducer<AdditionalBeanBuildItem> additionalBean;

    @Inject
    BuildProducer<AnnotatedWebsocketEndpointBuildItem> websocketProducer;

    @BuildStep
    @Record(STATIC_INIT)
    void createWebsocketEndpoints() {
        Config config = ConfigProvider.getConfig();

        Set<String> connectors = connectorNames(config, QuarkusWebsocketConnector.NAME);
        connectors
                .forEach(name -> generateWebsocketEndpoint(name, config));

        if (connectors.size() > 0) {
            additionalBean
                    .produce(new AdditionalBeanBuildItem(QuarkusWebsocketConnector.class));
        }
    }

    private void generateWebsocketEndpoint(String channelName, Config config) {
        String className = toUniqueClassName("io.quarkus.reactivemessaging.http.ReactiveWsEndpoint", channelName);

        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedBean.produce(new GeneratedBeanBuildItem(className, data));
            }
        };

        try (ClassCreator endpointCreator = ClassCreator.builder().classOutput(classOutput).className(className)
                .superClass(ReactiveWebsocketEndpointBase.class)
                .build()) {

            String path = config.getOptionalValue(configKey(channelName, "path"), String.class)
                    .orElseThrow(
                            () -> new IllegalArgumentException(
                                    "No path defined for reactive websocket endpoint " + channelName));
            endpointCreator.addAnnotation(ServerEndpoint.class)
                    .addValue("value", path);

            // mstodo register encoders and decoders if need be

            MethodCreator pathMethod = endpointCreator.getMethodCreator("path", String.class);
            ResultHandle pathHandle = pathMethod.load(path);
            pathMethod.returnValue(pathHandle);
        }
        websocketProducer.produce(new AnnotatedWebsocketEndpointBuildItem(className, false));
    }
}
