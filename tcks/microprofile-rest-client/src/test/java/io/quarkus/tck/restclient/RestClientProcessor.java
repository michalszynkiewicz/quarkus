package io.quarkus.tck.restclient;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class RestClientProcessor implements ApplicationArchiveProcessor {
    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        // mstodo check if needed
        String appProperties = "quarkus.index-dependency.tck.group-id=org.eclipse.microprofile.rest.client\n" +
                "quarkus.index-dependency.tck.artifact-id=microprofile-rest-client-tck\n";
        /*
         * // Only apply the processor to SSL tests
         * if (testClass.getName().contains("SslHostnameVerifierTest") ||
         * testClass.getName().contains("SslMutualTest") ||
         * testClass.getName().contains("SslTrustStoreTest") ||
         * testClass.getName().contains("SslContextTest")) {
         * 
         * appProperties += "quarkus.ssl.native=true\n";
         * }
         */

        if (applicationArchive instanceof WebArchive) {
            WebArchive war = applicationArchive.as(WebArchive.class);
            war.addAsResource(new StringAsset(appProperties), "application.properties");
        }

        // Make sure the test class and all of its superclasses are added to the test deployment
        // This ensures that all the classes from the hierarchy are loaded by the RuntimeClassLoader
        if (ClassContainer.class.isInstance(applicationArchive) && testClass.getJavaClass().getSuperclass() != null) {
            ClassContainer<?> classContainer = ClassContainer.class.cast(applicationArchive);
            Class<?> clazz = testClass.getJavaClass().getSuperclass();
            while (clazz != Object.class && clazz != null) {
                classContainer.addClass(clazz);
                clazz = clazz.getSuperclass();
            }

        }
    }
}
