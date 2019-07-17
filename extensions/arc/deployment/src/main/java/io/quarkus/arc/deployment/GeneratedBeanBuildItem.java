package io.quarkus.arc.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A generated CDI bean. If this is produced then a {@link io.quarkus.deployment.builditem.GeneratedClassBuildItem}
 * should not be produced for the same class, as Arc will take care of this.
 */
public final class GeneratedBeanBuildItem extends MultiBuildItem {

    final boolean applicationClass;
    final String name;
    final byte[] data;

    public GeneratedBeanBuildItem(String name, byte[] data) {
        this(false, name, data);
    }

    public GeneratedBeanBuildItem(boolean applicationClass, String name, byte[] data) {
        this.applicationClass = applicationClass;
        this.name = name;
        this.data = data;
    }

    public boolean isApplicationClass() {
        return applicationClass;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }
}
