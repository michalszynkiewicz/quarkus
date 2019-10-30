package io.quarkus.security.runtime.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Singleton
public class SecurityConstrainer {

    private final Map<Method, Optional<SecurityCheck>> checkForMethod = new ConcurrentHashMap<>();

    @Inject
    SecurityIdentity identity;

    @Inject
    SecurityCheckStorage storage;

    public void checkRoles(Method method, Collection<Annotation> interceptorBindings) {
        SecurityCheck securityCheck = storage.getSecurityAnnotation(method);

        if (securityCheck != null) {
            securityCheck.apply(identity);
        }
    }
}
