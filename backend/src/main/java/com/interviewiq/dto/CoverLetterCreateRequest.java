package com.interviewiq.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CoverLetterCreateRequest(
        @NotNull(message = "resumeId is required")
        UUID resumeId,
        @NotNull(message = "jobId is required")
        UUID jobId,
        @Size(max = 200, message = "candidateName must be 200 characters or fewer")
        String candidateName
) {
}