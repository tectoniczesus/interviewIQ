package com.interviewiq.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.interviewiq.exception.InvalidAiResponseException;

import java.util.List;

/**
 * Mirrors the exact JSON schema ResumeAnalysisPromptBuilder instructs Groq
 * to return. Never persisted or returned to the client until validate()
 * passes - see spec section 21 ("do not blindly persist malformed AI
 * responses").
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResumeAnalysisAiResponse(
        Integer atsScore,
        Integer overallScore,
        List<String> strengths,
        List<String> weaknesses,
        List<String> missingSkills,
        List<String> formattingIssues,
        List<String> keywordSuggestions,
        List<String> improvementSuggestions
) {
    public void validate() {
        requireScore(atsScore, "atsScore");
        requireScore(overallScore, "overallScore");
        requireList(strengths, "strengths");
        requireList(weaknesses, "weaknesses");
        requireList(missingSkills, "missingSkills");
        requireList(formattingIssues, "formattingIssues");
        requireList(keywordSuggestions, "keywordSuggestions");
        requireList(improvementSuggestions, "improvementSuggestions");
    }

    private static void requireScore(Integer score, String field) {
        if (score == null || score < 0 || score > 100) {
            throw new InvalidAiResponseException(
                    "AI response field '" + field + "' must be an integer between 0 and 100");
        }
    }

    private static void requireList(List<String> list, String field) {
        if (list == null) {
            throw new InvalidAiResponseException("AI response is missing required field '" + field + "'");
        }
    }
}