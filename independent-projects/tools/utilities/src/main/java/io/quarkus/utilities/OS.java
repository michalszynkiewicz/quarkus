package io.quarkus.utilities;

/**
 * Enum to classify the os.name system property
 */
public enum OS {
    WINDOWS,
    LINUX,
    MAC,
    OTHER;

    private String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    static OS determineOS() {
        final String osName = System.getProperty("os.name").toLowerCase();
        final OS os;
        if (osName.contains("windows")) {
            os = OS.WINDOWS;
        } else if (osName.contains("linux")
                || osName.contains("freebsd")
                || osName.contains("unix")
                || osName.contains("sunos")
                || osName.contains("solaris")
                || osName.contains("aix")) {
            os = OS.LINUX;
        } else if (osName.contains("mac os")) {
            os = OS.MAC;
        } else {
            os = OS.OTHER;
        }

        os.setVersion(System.getProperty("os.version"));
        return os;
    }
}
