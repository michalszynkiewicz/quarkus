package io.quarkus.reactivemessaging.http;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.reactivemessaging.http.ReactiveHttpConnectorUtils.configKey;
import static io.quarkus.reactivemessaging.http.ReactiveHttpConnectorUtils.connectorNames;
import static io.quarkus.reactivemessaging.http.ReactiveHttpConnectorUtils.toClassSafeCharacters;

import java.util.Random;

import javax.inject.Inject;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
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

        long connectorCount = connectorNames(config, QuarkusWebsocketConnector.NAME)
                .peek(name -> generateWebsocketEndpoint(name, config))
                .count();

        if (connectorCount > 0) {
            additionalBean
                    .produce(new AdditionalBeanBuildItem(QuarkusWebsocketConnector.class));
        }
    }

    private void generateWebsocketEndpoint(String connectorName, Config config) {
        String className = toUniqueClassName(connectorName);

        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                // mstodo rollback new constructor if not needed
                generatedBean.produce(new GeneratedBeanBuildItem(true, className, data));
            }
        };

        try (ClassCreator endpointCreator = ClassCreator.builder().classOutput(classOutput).className(className)
                .superClass(ReactiveWebsocketEndpointBase.class)
                .build()) {

            String path = config.getOptionalValue(configKey(connectorName, "path"), String.class)
                    .orElseThrow(
                            () -> new IllegalStateException(
                                    "No path defined for reactive websocket endpoint " + connectorName));
            endpointCreator.addAnnotation(ServerEndpoint.class)
                    .addValue("value", path);

            // mstodo register encoders and decoders if need be

            FieldCreator connector = endpointCreator.getFieldCreator("connector", QuarkusWebsocketConnector.class);
            connector.addAnnotation(Connector.class);

            MethodCreator connectorMethod = endpointCreator.getMethodCreator("connector", QuarkusWebsocketConnector.class);
            ResultHandle connectorHandle = connectorMethod.readInstanceField(connector.getFieldDescriptor(),
                    connectorMethod.getThis());
            connectorMethod.returnValue(connectorHandle);

            MethodCreator pathMethod = endpointCreator.getMethodCreator("path", String.class);
            ResultHandle pathHandle = pathMethod.load(path);
            pathMethod.returnValue(pathHandle);
        }
        websocketProducer.produce(new AnnotatedWebsocketEndpointBuildItem(className, false));
    }

    /*
     * 
     * 
     * @Connector(QuarkusWebsocketConnector.NAME)
     * QuarkusWebsocketConnector connector;
     */

    private String toUniqueClassName(String connectorName) {
        long randomNumber = random.nextInt(1000);
        return "io.quarkus.reactivemessaging.http.ReactiveWsEndpoint" + toClassSafeCharacters(connectorName) + randomNumber;
    }
}
