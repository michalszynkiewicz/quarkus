package io.quarkus.reactivemessaging.http;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 17/07/2019
 */
public class ReactiveHttpConnectorUtils {

    public static final String MP_MESSAGING_PREFIX = "mp.messaging.incoming.";
    public static final String CONNECTOR = ".connector";

    public static Stream<String> connectorNames(Config config, String connectorType) {
        Iterable<String> propertyKeys = config.getPropertyNames();
        return StreamSupport.stream(propertyKeys.spliterator(), false)
                .filter(ReactiveHttpConnectorUtils::isConnectorProperty)
                .filter(key -> isConnectorOfType(config, key, connectorType))
                .map(ReactiveHttpConnectorUtils::channelName);
    }

    public static String configKey(String channelName, String key) {
        return String.format("%s%s.%s", MP_MESSAGING_PREFIX, channelName, key);
    }

    public static String toClassSafeCharacters(String connectorName) {
        StringBuilder result = new StringBuilder();

        for (char c : connectorName.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                result.append(c);
            } else {
                result.append("_");
            }
        }

        return result.toString();
    }

    private static String channelName(String name) {
        return name.substring(MP_MESSAGING_PREFIX.length(), name.length() - CONNECTOR.length()).trim();
    }

    private static boolean isConnectorProperty(String name) {
        return name.startsWith(MP_MESSAGING_PREFIX) && name.endsWith(CONNECTOR);
    }

    private static boolean isConnectorOfType(Config config, String typeProperty, String connectorType) {
        return config.getOptionalValue(typeProperty, String.class)
                .orElse("")
                .equals(connectorType);
    }

    private ReactiveHttpConnectorUtils() {
    }
}
