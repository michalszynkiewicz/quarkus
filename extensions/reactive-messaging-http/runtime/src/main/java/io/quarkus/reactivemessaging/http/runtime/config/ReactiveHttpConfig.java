package io.quarkus.reactivemessaging.http.runtime.config;

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
    private static final String REACTIVE_STREAM_KEY = "mp.messaging.incoming.%s.%s";
    private static final Pattern REACTIVE_STREAM_PATTERN = Pattern.compile(quote(MP_MSG_IN) + "[^.]+" + quote(CONNECTOR));

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
        httpConfigs = readHttpConfigs();
        websocketConfigs = readWebsocketConfigs();
    }

    public static List<HttpStreamConfig> readHttpConfigs() {
        List<HttpStreamConfig> streamConfigs = new ArrayList<>();
        Config config = ConfigProviderResolver.instance().getConfig();
        for (String propertyName : config.getPropertyNames()) {

            Matcher matcher = REACTIVE_STREAM_PATTERN.matcher(propertyName);
            if (matcher.matches()) {
                String connectorName = propertyName.substring(MP_MSG_IN.length(), propertyName.length() - CONNECTOR.length());
                String connectorType = getConfigProperty(connectorName, "connector", "");
                if (QuarkusHttpConnector.NAME.equals(connectorType)) {
                    String method = getConfigProperty(connectorName, "method");
                    //                    String contentType = getConfigProperty(connectorName, "content-type"); // mstodo needed or not?
                    String path = getConfigProperty(connectorName, "path");
                    streamConfigs.add(new HttpStreamConfig(path, method, connectorName));
                }
            }
        }
        return streamConfigs;
    }

    public static List<WebsocketStreamConfig> readWebsocketConfigs() {
        List<WebsocketStreamConfig> streamConfigs = new ArrayList<>();
        Config config = ConfigProviderResolver.instance().getConfig();
        for (String propertyName : config.getPropertyNames()) {

            Matcher matcher = REACTIVE_STREAM_PATTERN.matcher(propertyName);
            if (matcher.matches()) {
                String connectorName = propertyName.substring(MP_MSG_IN.length(), propertyName.length() - CONNECTOR.length());
                String connectorType = getConfigProperty(connectorName, "connector", "");
                if (QuarkusWebsocketConnector.NAME.equals(connectorType)) {
                    String path = getConfigProperty(connectorName, "path");
                    streamConfigs.add(new WebsocketStreamConfig(path));
                }
            }
        }
        return streamConfigs;
    }

    private static String getConfigProperty(String connectorName, String property, String defValue) {
        String key = String.format(REACTIVE_STREAM_KEY, connectorName, property);
        return ConfigProvider.getConfig().getOptionalValue(key, String.class).orElse(defValue);
    }

    private static String getConfigProperty(String connectorName, String property) {
        String key = String.format(REACTIVE_STREAM_KEY, connectorName, property);
        return ConfigProvider.getConfig().getOptionalValue(key, String.class)
                .orElseThrow(() -> noPropertyFound(connectorName, property));
    }

    private static IllegalStateException noPropertyFound(String key, String propertyName) {
        String message = String.format("No %s defined for reactive http connector '%s'", propertyName, key);
        return new IllegalStateException(message);
    }
}
