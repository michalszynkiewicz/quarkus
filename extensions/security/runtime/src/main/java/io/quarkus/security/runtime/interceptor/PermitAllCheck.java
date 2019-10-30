package io.quarkus.security.runtime.interceptor;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class PermitAllCheck implements SecurityCheck {
    @Override
    public void apply(SecurityIdentity identity) {
    }
}