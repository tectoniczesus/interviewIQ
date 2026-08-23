package com.interviewiq.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResumeAnalysisResponse(
        UUID id,
        UUID resumeId,
        int atsScore,
        int overallScore,
        List<String> strengths,
        List<String> weaknesses,
        List<String> missingSkills,
        List<String> formattingIssues,
        List<String> keywordSuggestions,
        List<String> improvementSuggestions,
        Instant createdAt
) {
}