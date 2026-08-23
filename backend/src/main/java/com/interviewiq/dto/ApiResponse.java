package com.interviewiq.dto;

/**
 * Wraps every successful controller response as { "success": true, "data": ... }
 * so the frontend has one consistent shape to unwrap. Failures are handled
 * separately by GlobalExceptionHandler, which returns the more detailed
 * error envelope (timestamp/status/error/message/path).
 */
public record ApiResponse<T>(boolean success, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }
}
