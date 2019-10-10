package io.quarkus.vertx.http.runtime.security;

import java.util.concurrent.CompletionStage;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

public interface HTTPAuthenticationMechanism {

    CompletionStage<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager);

    CompletionStage<Boolean> sendChallenge(RoutingContext context);

    int challengeStatus();

    String challengeContent();

    default CharSequence challengeHeader() {
        return HttpHeaderNames.WWW_AUTHENTICATE;
    }
}
