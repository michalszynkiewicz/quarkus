package io.quarkus.security.runtime.interceptor;

import static java.util.Arrays.asList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.ForbiddenException;
import io.quarkus.security.runtime.UnauthorizedException;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 03/10/2019
 */
@Singleton
public class SecurityConstrainer {

    private static final List<Class<? extends Annotation>> SECURITY_ANNOTATIONS = asList(Authenticated.class, DenyAll.class,
            PermitAll.class, RolesAllowed.class);

    private final Map<Method, Optional<Check>> checkForMethod = new HashMap<>();

    @Inject
    SecurityIdentity identity;

    public void checkRoles(Method method, Collection<Annotation> interceptorBindings) {
        getCheck(method, interceptorBindings)
                .ifPresent(check -> check.apply(identity));
    }

    private Optional<Check> getCheck(Method method, Collection<Annotation> interceptorBindings) {
        return checkForMethod.computeIfAbsent(method, m -> determineSecurityCheck(m, interceptorBindings));
    }

    private Optional<Check> determineSecurityCheck(Method method, Collection<Annotation> interceptorBindings) {
        Annotation securityAnnotation = determineSecurityAnnotation(method.getDeclaredAnnotations(), method::toString);
        if (securityAnnotation == null) {
            Class<?> declaringClass = method.getDeclaringClass();
            securityAnnotation = determineSecurityAnnotation(declaringClass.getDeclaredAnnotations(),
                    declaringClass::getCanonicalName);
        }
        if (securityAnnotation == null) {
            securityAnnotation = determineSecurityAnnotationFromBindings(interceptorBindings, method::toString);
        }
        return checkForAnnotation(securityAnnotation);
    }

    private Optional<Check> checkForAnnotation(Annotation securityAnnotation) {
        if (securityAnnotation instanceof DenyAll) {
            return Optional.of(new DenyAllCheck());
        }
        if (securityAnnotation instanceof RolesAllowed) {
            RolesAllowed rolesAllowed = (RolesAllowed) securityAnnotation;
            return Optional.of(new RolesAllowedCheck(rolesAllowed.value()));
        }
        if (securityAnnotation instanceof PermitAll) {
            return Optional.of(new PermitAllCheck());
        }
        if (securityAnnotation instanceof Authenticated) {
            return Optional.of(new AuthenticatedCheck());
        }
        return Optional.empty();
    }

    private Annotation determineSecurityAnnotationFromBindings(Collection<Annotation> interceptorBindings,
            Supplier<String> annotationPlacement) {
        List<Annotation> securityAnnotations = interceptorBindings.stream()
                .filter(anno -> SECURITY_ANNOTATIONS.stream().anyMatch(c -> c.isInstance(anno)))
                .collect(Collectors.toList());
        return getExactlyOne(securityAnnotations, annotationPlacement);
    }

    private Annotation determineSecurityAnnotation(Annotation[] annotations, Supplier<String> annotationPlacement) {
        List<Annotation> securityAnnotations = Stream.of(annotations)
                .filter(ann -> SECURITY_ANNOTATIONS.contains(ann.annotationType()))
                .collect(Collectors.toList());

        return getExactlyOne(securityAnnotations, annotationPlacement);
    }

    private Annotation getExactlyOne(List<Annotation> securityAnnotations, Supplier<String> annotationPlacement) {
        switch (securityAnnotations.size()) {
            case 0:
                return null;
            case 1:
                return securityAnnotations.get(0);
            default:
                throw new IllegalStateException("Duplicate security annotations found on "
                        + annotationPlacement.get() +
                        ". Expected at most 1 annotation, found: " + securityAnnotations);
        }
    }

    private static class RolesAllowedCheck implements Check {
        private final String[] allowedRoles;

        private RolesAllowedCheck(String[] allowedRoles) {
            this.allowedRoles = allowedRoles;
        }

        @Override
        public void apply(SecurityIdentity identity) {
            if (identity.isAnonymous()) {
                throw new UnauthorizedException();
            }
            for (String role : allowedRoles) {
                Set<String> roles = identity.getRoles();
                if (roles.contains(role)) {
                    return;
                }
            }
            throw new ForbiddenException();
        }
    }

    private static class DenyAllCheck implements Check {
        @Override
        public void apply(SecurityIdentity identity) {
            if (identity.isAnonymous()) {
                throw new UnauthorizedException();
            } else {
                throw new ForbiddenException();
            }
        }
    }

    private static class PermitAllCheck implements Check {
        @Override
        public void apply(SecurityIdentity identity) {
        }
    }

    private static class AuthenticatedCheck implements Check {

        @Override
        public void apply(SecurityIdentity identity) {
            if (identity.isAnonymous()) {
                throw new UnauthorizedException();
            }
        }
    }

    private interface Check {
        void apply(SecurityIdentity identity);
    }
}
