package com.interviewiq.dto;

import java.time.Instant;
import java.util.UUID;

public record DashboardJobMatchItem(
        UUID jobMatchId,
        UUID jobId,
        String jobTitle,
        String company,
        int matchScore,
        Instant createdAt
) {
}