package com.interviewiq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds app.clerk.* properties. Consumed by the security package to
 * validate incoming Clerk session JWTs on every request.
 */
@Configuration
@ConfigurationProperties(prefix = "app.clerk")
public class ClerkConfig {

    private String secretKey;
    private String jwtIssuer;
    private String jwksUrl;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }

    public void setJwtIssuer(String jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    public String getJwksUrl() {
        return jwksUrl;
    }

    public void setJwksUrl(String jwksUrl) {
        this.jwksUrl = jwksUrl;
    }
}
