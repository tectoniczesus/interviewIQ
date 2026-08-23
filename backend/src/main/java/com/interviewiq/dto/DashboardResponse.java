package com.interviewiq.dto;

import java.util.List;

public record DashboardResponse(
        Integer resumeScore,
        long resumeCount,
        long jobMatchCount,
        long coverLetterCount,
        long interviewSessionCount,
        List<DashboardResumeItem> recentResumes,
        List<DashboardJobMatchItem> recentJobMatches
) {
}