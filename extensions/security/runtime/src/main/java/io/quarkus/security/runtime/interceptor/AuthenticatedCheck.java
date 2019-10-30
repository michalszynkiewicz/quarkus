package io.quarkus.security.runtime.interceptor;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class AuthenticatedCheck implements SecurityCheck {

    @Override
    public void apply(SecurityIdentity identity) {
        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        }
    }
}
