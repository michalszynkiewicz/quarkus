package io.quarkus.security.runtime.interceptor;

import java.lang.reflect.Method;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public interface SecurityCheckStorage {
    SecurityCheck getSecurityAnnotation(Method method);
}
