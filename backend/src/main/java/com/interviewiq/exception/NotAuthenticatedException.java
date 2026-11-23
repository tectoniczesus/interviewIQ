package com.interviewiq.exception;

/**
 * Thrown specifically when a service/controller expects an authenticated
 * caller (via CurrentUserProvider) but the security context has none.
 * Deliberately its own type - a bare IllegalStateException is also thrown
 * elsewhere in the app for unrelated reasons (JWKS fetch failure, corrupt
 * stored data), and conflating those with "please log in again" would give
 * the client a misleading response for what's actually a server bug.
 */
public class NotAuthenticatedException extends RuntimeException {
    public NotAuthenticatedException(String message) {
        super(message);
    }
}