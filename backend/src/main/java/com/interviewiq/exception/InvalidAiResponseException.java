package com.interviewiq.exception;

/**
 * Thrown when the AI (Groq) returns a response that does not conform to
 * the expected JSON schema. Controllers should never persist or forward
 * an AI response that has triggered this exception.
 */
public class InvalidAiResponseException extends RuntimeException {
    public InvalidAiResponseException(String message) {
        super(message);
    }

    public InvalidAiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
