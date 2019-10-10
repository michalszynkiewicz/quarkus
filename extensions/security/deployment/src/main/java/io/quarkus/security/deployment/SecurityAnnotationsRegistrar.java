package io.quarkus.security.deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.security.Authenticated;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 03/10/2019
 */
@Singleton
public class SecurityAnnotationsRegistrar implements InterceptorBindingRegistrar {
    @Override
    public Map<DotName, Set<String>> registerAdditionalBindings() {
        Map<DotName, Set<String>> newBindings = new HashMap<>();

        newBindings.put(DotName.createSimple(RolesAllowed.class.getName()), Collections.singleton("value"));
        newBindings.put(DotName.createSimple(Authenticated.class.getName()), Collections.emptySet());
        newBindings.put(DotName.createSimple(DenyAll.class.getName()), Collections.emptySet());
        newBindings.put(DotName.createSimple(PermitAll.class.getName()), Collections.emptySet());
        return newBindings;
    }
}
