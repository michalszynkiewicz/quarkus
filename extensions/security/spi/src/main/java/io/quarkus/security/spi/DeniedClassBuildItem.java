package io.quarkus.security.spi;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public final class DeniedClassBuildItem extends MultiBuildItem {
    private final DotName name;

    public DeniedClassBuildItem(DotName name) {
        this.name = name;
    }

    public DotName getName() {
        return name;
    }
}
