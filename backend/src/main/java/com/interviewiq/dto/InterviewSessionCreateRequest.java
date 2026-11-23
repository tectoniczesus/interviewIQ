package com.interviewiq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record InterviewSessionCreateRequest(
        @NotNull(message = "resumeId is required")
        UUID resumeId,
        @NotNull(message = "jobId is required")
        UUID jobId,
        @NotBlank(message = "role is required")
        @Size(max = 200, message = "role must be 200 characters or fewer")
        String role
) {
}