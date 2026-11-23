package com.interviewiq.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LearningRoadmapResponse(
        UUID id,
        UUID jobMatchId,
        UUID jobId,
        UUID resumeId,
        String jobTitle,
        String company,
        String resumeFileName,
        int matchScore,
        String summary,
        int hoursPerWeek,
        List<WeekResponse> weeks,
        int totalTasks,
        int completedTasks,
        int progressPercent,
        Instant createdAt,
        Instant updatedAt
) {
    public record WeekResponse(
            int week,
            String focus,
            List<String> skills,
            String milestone,
            List<TaskResponse> tasks
    ) {
    }

    public record TaskResponse(
            String id,
            String title,
            String description,
            String type,
            String resourceName,
            String searchQuery,
            boolean completed
    ) {
    }
}
