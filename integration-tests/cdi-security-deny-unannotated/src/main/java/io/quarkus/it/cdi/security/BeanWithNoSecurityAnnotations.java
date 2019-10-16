package io.quarkus.it.cdi.security;

import javax.inject.Singleton;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 16/10/2019
 */
@Singleton
public class BeanWithNoSecurityAnnotations {
    public String unannotated() {
        return "unannotatedOnBeanWithNoAnno";
    }
}
