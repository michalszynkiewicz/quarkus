package io.quarkus.security.runtime.interceptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import io.quarkus.security.Authenticated;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class SecurityCheckStorageBuilder {
    private final Map<MethodDescription, SecurityCheck> securityChecks = new HashMap<>();

    public void registerAnnotation(Class aClass,
            String methodName,
            Class[] parameterTypes,
            Class<?> securityAnnotation,
            String[] value) {
        securityChecks.put(new MethodDescription(aClass, methodName, parameterTypes),
                determineCheck(securityAnnotation, value));
    }

    public SecurityCheckStorage create() {
        return new SecurityCheckStorage() {
            @Override
            public SecurityCheck getSecurityAnnotation(Method method) {
                MethodDescription descriptor = new MethodDescription(method.getDeclaringClass(), method.getName(),
                        method.getParameterTypes());
                return securityChecks.get(descriptor);
            }
        };
    }

    private SecurityCheck determineCheck(Class securityAnnotation, String[] value) {
        if (securityAnnotation == DenyAll.class) {
            return new DenyAllCheck();
        }
        if (securityAnnotation == RolesAllowed.class) {
            return new RolesAllowedCheck(value);
        }
        if (securityAnnotation == PermitAll.class) {
            return new PermitAllCheck();
        }
        if (securityAnnotation == Authenticated.class) {
            return new AuthenticatedCheck();
        }
        throw new IllegalArgumentException("Unsupported security check " + securityAnnotation);
    }

    static class MethodDescription {
        private final String className;
        private final String methodName;
        private final String[] parameterTypes;

        public MethodDescription(Class aClass, String methodName, Class[] parameterTypes) {
            this.className = aClass.getName();
            this.methodName = methodName;
            this.parameterTypes = typesAsStrings(parameterTypes);
        }

        private String[] typesAsStrings(Class[] parameterTypes) {
            String[] result = new String[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                result[i] = parameterTypes[i].getName();
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            MethodDescription that = (MethodDescription) o;
            return className.equals(that.className) &&
                    methodName.equals(that.methodName) &&
                    Arrays.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(className, methodName);
            result = 31 * result + Arrays.hashCode(parameterTypes);
            return result;
        }
    }
}
