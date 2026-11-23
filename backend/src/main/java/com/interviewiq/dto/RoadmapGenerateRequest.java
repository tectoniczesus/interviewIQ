package com.interviewiq.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Optional body for roadmap generation; hoursPerWeek defaults to 8 when omitted. */
public record RoadmapGenerateRequest(
        @Min(value = 2, message = "hoursPerWeek must be at least 2")
        @Max(value = 40, message = "hoursPerWeek must be 40 or fewer")
        Integer hoursPerWeek
) {
}
