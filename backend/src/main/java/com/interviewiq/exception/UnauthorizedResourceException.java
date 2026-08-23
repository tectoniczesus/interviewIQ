package com.interviewiq.exception;

/**
 * Thrown when an authenticated user attempts to access a resource
 * (resume, job, analysis, etc.) that belongs to a different user.
 */
public class UnauthorizedResourceException extends RuntimeException {
    public UnauthorizedResourceException(String message) {
        super(message);
    }
}
