package io.quarkus.it.rest.client;

public class HeaderUtil {
    public static String generateHeader() {
        return HeaderPassingClient.STATIC_COMPUTED_VALUE;
    }
}
