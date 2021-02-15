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
        // TODO SSL properties may be needed when SSL tests are fixed
        String appProperties = "";

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
