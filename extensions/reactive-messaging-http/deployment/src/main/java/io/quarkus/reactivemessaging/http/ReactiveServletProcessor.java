package io.quarkus.reactivemessaging.http;

import static io.quarkus.reactivemessaging.http.ReactiveHttpConnectorUtils.configKey;
import static io.quarkus.reactivemessaging.http.ReactiveHttpConnectorUtils.connectorNames;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         Date: 11/07/2019
 */
class ReactiveServletProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.REACTIVE_MESSAGING_HTTP);
    }

    @BuildStep
    void registerDeserializers(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        Config config = ConfigProvider.getConfig();

        connectorNames(config, QuarkusHttpConnector.NAME)
                .forEach(name -> registerDeserializer(name, config, reflectiveClass));
    }

    @BuildStep
    void registerServlets(BuildProducer<ServletBuildItem> servlet,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        Config config = ConfigProvider.getConfig();

        long endpointCount = connectorNames(config, QuarkusHttpConnector.NAME)
                .peek(name -> setUpServlet(name, config, servlet))
                .count();
        if (endpointCount > 0) {
            additionalBean.produce(new AdditionalBeanBuildItem(ReactiveHttpServlet.class, QuarkusHttpConnector.class));
        }
    }

    private void registerDeserializer(String name, Config config, BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        config.getOptionalValue(configKey(name, "deserializer"), String.class)
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .ifPresent(reflectiveClass::produce);
    }

    private void setUpServlet(
            String channelName,
            Config config,
            BuildProducer<ServletBuildItem> servlet) {
        String servletName = "quarkus-reactive-messaging-" + channelName;

        String path = config.getOptionalValue(configKey(channelName, "path"), String.class)
                .orElseThrow(() -> new IllegalStateException("No path for channel " + channelName + " provided"));

        servlet.produce(ServletBuildItem.builder(servletName, ReactiveHttpServlet.class.getName())
                .addMapping(path).build());
    }

}
