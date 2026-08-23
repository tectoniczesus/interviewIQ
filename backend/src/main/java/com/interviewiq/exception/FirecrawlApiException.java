package com.interviewiq.exception;

public class FirecrawlApiException extends RuntimeException {
    public FirecrawlApiException(String message) {
        super(message);
    }

    public FirecrawlApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
