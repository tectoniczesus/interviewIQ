package com.interviewiq.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record JobMatchCreateRequest(
        @NotNull(message = "resumeId is required")
        UUID resumeId,
        @NotNull(message = "jobId is required")
        UUID jobId
) {
}