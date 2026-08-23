package com.interviewiq.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobMatchResponse(
        UUID id,
        UUID resumeId,
        UUID jobId,
        int matchScore,
        List<String> matchingSkills,
        List<String> missingSkills,
        List<String> matchingExperience,
        List<String> missingRequirements,
        List<String> keywordGaps,
        List<String> recommendations,
        Instant createdAt
) {
}