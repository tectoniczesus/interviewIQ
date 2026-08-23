package com.interviewiq.dto;

import java.time.Instant;
import java.util.UUID;

public record DashboardResumeItem(
        UUID id,
        String fileName,
        Integer atsScore,
        Instant createdAt
) {
}