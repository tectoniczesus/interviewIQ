package com.interviewiq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InterviewSessionCreateRequest(
        @NotNull(message = "resumeId is required")
        UUID resumeId,
        @NotNull(message = "jobId is required")
        UUID jobId,
        @NotBlank(message = "role is required")
        String role
) {
}