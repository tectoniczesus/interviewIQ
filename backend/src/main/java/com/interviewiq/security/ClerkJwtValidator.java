package com.interviewiq.security;

import com.interviewiq.config.ClerkConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import org.springframework.stereotype.Component;

import java.security.Key;

/**
 * Verifies the signature, issuer, and expiry of a Clerk session token and
 * turns it into an AuthenticatedUser. This is the ONLY place the backend
 * should decide who a request is coming from.
 */
@Component
public class ClerkJwtValidator {

    private final ClerkConfig clerkConfig;
    private final ClerkJwkProvider jwkProvider;

    public ClerkJwtValidator(ClerkConfig clerkConfig, ClerkJwkProvider jwkProvider) {
        this.clerkConfig = clerkConfig;
        this.jwkProvider = jwkProvider;
    }

    public AuthenticatedUser validate(String token) {
        Locator<Key> keyLocator = header -> {
            String kid = ((JwsHeader) header).getKeyId();
            return jwkProvider.getKey(kid);
        };

        Jws<Claims> jws = Jwts.parser()
                .keyLocator(keyLocator)
                .requireIssuer(clerkConfig.getJwtIssuer())
                .build()
                .parseSignedClaims(token);

        Claims claims = jws.getPayload();

        String clerkUserId = claims.getSubject();
        String email = claims.get("email", String.class);

        if (clerkUserId == null || clerkUserId.isBlank()) {
            throw new IllegalStateException("Clerk token is missing a subject (user id) claim");
        }

        return new AuthenticatedUser(clerkUserId, email);
    }
}
