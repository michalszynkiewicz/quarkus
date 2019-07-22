package io.quarkus.reactivemessaging.http;

import static io.quarkus.reactivemessaging.http.ReactiveHttpConnectorUtils.configKey;
import static io.quarkus.reactivemessaging.http.ReactiveHttpConnectorUtils.connectorNames;
import static io.quarkus.reactivemessaging.http.ReactiveHttpConnectorUtils.toUniqueClassName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.undertow.deployment.ServletBuildItem;

/**
 * <ul>
 * <li>json for content types containing json</li>
 * <li>String for content types containing text</li>
 * <li>byte[] for everything else</li>
 * </ul>
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         Date: 11/07/2019
 */
class ReactiveHttpProcessor {

    @Inject
    BuildProducer<GeneratedBeanBuildItem> generatedBean;

    @Inject
    BuildProducer<AdditionalBeanBuildItem> additionalBean;

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.REACTIVE_MESSAGING_HTTP);
    }

    @BuildStep
    void registerServlets(BuildProducer<ServletBuildItem> servlet,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        Config config = ConfigProvider.getConfig();

        Set<String> connectors = connectorNames(config, QuarkusHttpConnector.NAME);
        Map<String, List<HttpMethod>> methodsByPaths = new HashMap<>();
        for (String channelName : connectors) {
            String path = config.getOptionalValue(configKey(channelName, "path"), String.class)
                    .orElseThrow(() -> new IllegalStateException("No path for channel " + channelName + " provided"));
            String method = config.getOptionalValue(configKey(channelName, "method"), String.class)
                    .map(String::toUpperCase)
                    .orElseThrow(() -> new IllegalStateException("No method for channel " + channelName + " provided"));

            HttpMethod httpMethod;
            try {
                httpMethod = HttpMethod.valueOf(method);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Unsupported http method: " + method + " defined for channel: " + channelName);
            }

            methodsByPaths.computeIfAbsent(path, whatever -> new ArrayList<>())
                    .add(httpMethod);
        }

        for (Map.Entry<String, List<HttpMethod>> entry : methodsByPaths.entrySet()) {
            setUpServlet(entry.getKey(), entry.getValue(), servlet);
        }

        if (connectors.size() > 0) {
            additionalBean.produce(new AdditionalBeanBuildItem(QuarkusHttpConnector.class));
        }
    }

    private void setUpServlet(
            String path,
            List<HttpMethod> methods,
            BuildProducer<ServletBuildItem> servlet) {

        String className = toUniqueClassName("io.quarkus.reactivemessaging.http.ReactiveHttpServlet", path);

        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedBean.produce(new GeneratedBeanBuildItem(className, data));
            }
        };

        try (ClassCreator servletCreator = ClassCreator.builder().classOutput(classOutput).className(className)
                .superClass(ReactiveHttpServletBase.class)
                .build()) {

            servletCreator.addAnnotation(WebServlet.class);
            servletCreator.addAnnotation(ApplicationScoped.class);

            MethodCreator pathMethod = servletCreator.getMethodCreator("path", String.class);
            ResultHandle pathHandle = pathMethod.load(path);
            pathMethod.returnValue(pathHandle);

            MethodCreator methodsMethod = servletCreator.getMethodCreator("methods", HttpMethod[].class);
            ResultHandle[] methodsArray = new ResultHandle[methods.size()];
            for (int i = 0; i < methods.size(); i++) {
                methodsArray[i] = methodsMethod.load(methods.get(i));
            }
            ResultHandle methodsArrayHandle = methodsMethod.marshalAsArray(HttpMethod.class, methodsArray);
            methodsMethod.returnValue(methodsArrayHandle);
        }

        servlet.produce(ServletBuildItem.builder(className, className).addMapping(path).build());
    }
}
