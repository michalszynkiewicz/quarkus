package io.quarkus.security.runtime;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import io.quarkus.security.Authenticated;
import io.quarkus.security.runtime.interceptor.AuthenticatedCheck;
import io.quarkus.security.runtime.interceptor.DenyAllCheck;
import io.quarkus.security.runtime.interceptor.PermitAllCheck;
import io.quarkus.security.runtime.interceptor.RolesAllowedCheck;
import io.quarkus.security.runtime.interceptor.SecurityCheck;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public abstract class SecurityCheckStorage {
    // method desc -> annotation class
    private final Map<MethodDescription, CheckBuildTimeData> securityChecksBuildTimeData = new HashMap<>();
    private Map<MethodDescription, SecurityCheck> securityChecks;
    private boolean initialized = false;

    public void registerAnnotation(Class aClass,
            String methodName,
            Class[] parameterTypes,
            Class<?> securityAnnotation,
            String[] value) {
        CheckBuildTimeData data = new CheckBuildTimeData(securityAnnotation, value);
        securityChecksBuildTimeData.put(new MethodDescription(aClass, methodName, parameterTypes), data);
    }

    public SecurityCheck getSecurityAnnotation(Method method) {
        if (!initialized) {
            initialize();
        }
        MethodDescription descriptor = new MethodDescription(method.getDeclaringClass(), method.getName(),
                method.getParameterTypes());
        return securityChecks.get(descriptor);
    }

    private synchronized void initialize() {
        securityChecks = new HashMap<>();
        for (Map.Entry<MethodDescription, CheckBuildTimeData> checkEntry : securityChecksBuildTimeData.entrySet()) {
            securityChecks.put(checkEntry.getKey(), determineCheck(checkEntry.getValue()));
        }

        initialized = true;
    }

    private SecurityCheck determineCheck(CheckBuildTimeData checkData) {
        Class securityAnnotation = checkData.securityAnnotation;
        if (securityAnnotation == DenyAll.class) {
            return new DenyAllCheck();
        }
        if (securityAnnotation == RolesAllowed.class) {
            return new RolesAllowedCheck(checkData.value);
        }
        if (securityAnnotation == PermitAll.class) {
            return new PermitAllCheck();
        }
        if (securityAnnotation == Authenticated.class) {
            return new AuthenticatedCheck();
        }
        throw new IllegalArgumentException("Unsupported security check " + checkData.securityAnnotation);
    }

    private static class CheckBuildTimeData {
        Class securityAnnotation;
        String[] value;

        public CheckBuildTimeData(Class<?> securityAnnotation, String[] value) {
            this.securityAnnotation = securityAnnotation;
            this.value = value;
        }
    }

    private static class MethodDescription {
        private final Class aClass;
        private final String methodName;
        private final Class[] parameterTypes;

        public MethodDescription(Class aClass, String methodName, Class<?>[] parameterTypes) {
            this.aClass = aClass;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            MethodDescription that = (MethodDescription) o;
            return aClass.equals(that.aClass) &&
                    methodName.equals(that.methodName) &&
                    Arrays.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(aClass, methodName);
            result = 31 * result + Arrays.hashCode(parameterTypes);
            return result;
        }
    }
}
