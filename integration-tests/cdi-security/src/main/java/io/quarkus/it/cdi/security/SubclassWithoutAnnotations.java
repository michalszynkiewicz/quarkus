package io.quarkus.it.cdi.security;

import javax.enterprise.context.ApplicationScoped;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 07/10/2019
 */
@ApplicationScoped
public class SubclassWithoutAnnotations extends SubclassWithDenyAll {
}
