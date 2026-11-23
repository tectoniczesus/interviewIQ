package com.interviewiq.dto;

import jakarta.validation.constraints.NotNull;

public record RoadmapTaskUpdateRequest(
        @NotNull(message = "completed is required")
        Boolean completed
) {
}
