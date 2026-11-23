package com.interviewiq.dto;

import java.time.Instant;
import java.util.UUID;

/** List-view shape: progress only, without the weeks/tasks payload. */
public record LearningRoadmapSummaryResponse(
        UUID id,
        UUID jobMatchId,
        UUID jobId,
        String jobTitle,
        String company,
        String resumeFileName,
        int matchScore,
        int weekCount,
        int totalTasks,
        int completedTasks,
        int progressPercent,
        Instant updatedAt
) {
}
