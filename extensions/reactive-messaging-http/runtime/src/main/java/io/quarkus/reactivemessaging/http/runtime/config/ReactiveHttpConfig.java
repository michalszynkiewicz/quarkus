package io.quarkus.reactivemessaging.http.runtime.config;

import static io.quarkus.reactivemessaging.http.runtime.QuarkusHttpConnector.DEFAULT_SOURCE_BUFFER;
import static java.util.regex.Pattern.quote;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.reactivemessaging.http.runtime.QuarkusHttpConnector;
import io.quarkus.reactivemessaging.http.runtime.QuarkusWebsocketConnector;

@Singleton
public class ReactiveHttpConfig {
    private static final String CONNECTOR = ".connector";

    private static final String MP_MSG_IN = "mp.messaging.incoming.";
    private static final String IN_KEY = "mp.messaging.incoming.%s.%s";
    private static final Pattern IN_PATTERN = Pattern.compile(quote(MP_MSG_IN) + "[^.]+" + quote(CONNECTOR));

    private static final String MP_MSG_OUT = "mp.messaging.outgoing.";
    private static final String OUT_KEY = "mp.messaging.outgoing.%s.%s";
    private static final Pattern OUT_PATTERN = Pattern.compile(quote(MP_MSG_OUT) + "[^.]+" + quote(CONNECTOR));

    private List<HttpStreamConfig> httpConfigs;
    private List<WebsocketStreamConfig> websocketConfigs;

    public List<HttpStreamConfig> getHttpConfigs() {
        return httpConfigs;
    }

    public List<WebsocketStreamConfig> getWebsocketConfigs() {
        return websocketConfigs;
    }

    @PostConstruct
    public void init() {
        httpConfigs = readIncomingHttpConfigs();
        websocketConfigs = readIncomingWebsocketConfigs();
    }

    public static List<HttpStreamConfig> readIncomingHttpConfigs() {
        List<HttpStreamConfig> streamConfigs = new ArrayList<>();
        Config config = ConfigProviderResolver.instance().getConfig();
        for (String propertyName : config.getPropertyNames()) {
            String connectorName = getConnectorNameIfMatching(IN_PATTERN, propertyName, IN_KEY, MP_MSG_IN,
                    QuarkusHttpConnector.NAME);
            if (connectorName != null) {
                String method = getConfigProperty(IN_KEY, connectorName, "method", String.class);
                String path = getConfigProperty(IN_KEY, connectorName, "path", String.class);
                int bufferSize = getConfigProperty(IN_KEY, connectorName, "buffer-size", DEFAULT_SOURCE_BUFFER,
                        Integer.class) + 1;
                streamConfigs.add(new HttpStreamConfig(path, method, connectorName, bufferSize));
            }
        }
        return streamConfigs;
    }

    public static List<WebsocketStreamConfig> readIncomingWebsocketConfigs() {
        List<WebsocketStreamConfig> streamConfigs = new ArrayList<>();
        Config config = ConfigProviderResolver.instance().getConfig();
        for (String propertyName : config.getPropertyNames()) {
            // mstodo simplify it!
            String connectorName = getConnectorNameIfMatching(IN_PATTERN, propertyName, IN_KEY, MP_MSG_IN,
                    QuarkusWebsocketConnector.NAME);

            if (connectorName != null) {
                String path = getConfigProperty(IN_KEY, connectorName, "path", String.class);
                streamConfigs.add(new WebsocketStreamConfig(path));
            }
        }
        return streamConfigs;
    }

    public static List<String> readSerializers() {
        List<String> result = new ArrayList<>();
        Config config = ConfigProviderResolver.instance().getConfig();
        for (String propertyName : config.getPropertyNames()) {
            String connectorName = getConnectorNameIfMatching(OUT_PATTERN, propertyName, OUT_KEY, MP_MSG_OUT,
                    QuarkusWebsocketConnector.NAME);
            if (connectorName == null) {
                connectorName = getConnectorNameIfMatching(OUT_PATTERN, propertyName, OUT_KEY, MP_MSG_OUT,
                        QuarkusHttpConnector.NAME);
            }
            if (connectorName != null) {
                String serializer = getConfigProperty(OUT_KEY, connectorName, "serializer", null);
                if (serializer != null) {
                    result.add(serializer);
                }
            }
        }
        return result;
    }

    private static String getConnectorNameIfMatching(Pattern connectorPropertyPattern,
            String propertyName, String format, String prefix, String expectedConnectorType) {
        Matcher matcher = connectorPropertyPattern.matcher(propertyName);
        if (matcher.matches()) {
            String connectorName = propertyName.substring(prefix.length(), propertyName.length() - CONNECTOR.length());
            String connectorType = getConfigProperty(format, connectorName, "connector", String.class);
            boolean matches = expectedConnectorType.equals(connectorType);
            return matches ? connectorName : null;
        } else {
            return null;
        }
    }

    private static <T> T getConfigProperty(String format, String connectorName, String property, T defValue, Class<T> type) {
        String key = String.format(format, connectorName, property);
        return ConfigProvider.getConfig().getOptionalValue(key, type).orElse(defValue);
    }

    private static <T> T getConfigProperty(String format, String connectorName, String property, Class<T> type) {
        String key = String.format(format, connectorName, property);
        return ConfigProvider.getConfig().getOptionalValue(key, type)
                .orElseThrow(() -> noPropertyFound(connectorName, property));
    }

    private static IllegalStateException noPropertyFound(String key, String propertyName) {
        String message = String.format("No %s defined for reactive http connector '%s'", propertyName, key);
        return new IllegalStateException(message);
    }
}
