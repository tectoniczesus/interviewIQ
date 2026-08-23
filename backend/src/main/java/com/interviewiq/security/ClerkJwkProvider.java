package com.interviewiq.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.config.ClerkConfig;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches Clerk's JSON Web Key Set (JWKS) and caches the public keys by
 * "kid" so incoming session tokens can be verified without calling Clerk
 * on every request. Keys are refreshed periodically since Clerk rotates
 * them.
 */
@Component
public class ClerkJwkProvider {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final ClerkConfig clerkConfig;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, PublicKey> keysByKid = new ConcurrentHashMap<>();
    private volatile Instant lastFetchedAt = Instant.EPOCH;

    public ClerkJwkProvider(ClerkConfig clerkConfig) {
        this.clerkConfig = clerkConfig;
    }

    public PublicKey getKey(String kid) {
        if (Duration.between(lastFetchedAt, Instant.now()).compareTo(CACHE_TTL) > 0
                || !keysByKid.containsKey(kid)) {
            refresh();
        }

        PublicKey key = keysByKid.get(kid);
        if (key == null) {
            throw new IllegalStateException("No matching JWKS key found for kid: " + kid);
        }
        return key;
    }

    private synchronized void refresh() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(clerkConfig.getJwksUrl()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            for (JsonNode jwk : root.get("keys")) {
                String kid = jwk.get("kid").asText();
                String n = jwk.get("n").asText();
                String e = jwk.get("e").asText();
                keysByKid.put(kid, toPublicKey(n, e));
            }

            lastFetchedAt = Instant.now();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to refresh Clerk JWKS from " + clerkConfig.getJwksUrl(), ex);
        }
    }

    private PublicKey toPublicKey(String modulusB64Url, String exponentB64Url) throws Exception {
        Base64.Decoder decoder = Base64.getUrlDecoder();
        BigInteger modulus = new BigInteger(1, decoder.decode(modulusB64Url));
        BigInteger exponent = new BigInteger(1, decoder.decode(exponentB64Url));
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }
}
