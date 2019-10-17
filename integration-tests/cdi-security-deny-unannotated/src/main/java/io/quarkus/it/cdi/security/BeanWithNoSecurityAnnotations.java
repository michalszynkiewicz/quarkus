package io.quarkus.it.cdi.security;

import javax.inject.Singleton;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Singleton
public class BeanWithNoSecurityAnnotations {
    public String unannotated() {
        return "unannotatedOnBeanWithNoAnno";
    }
}
