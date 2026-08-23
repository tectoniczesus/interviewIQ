package com.interviewiq.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.interviewiq.exception.InvalidAiResponseException;

import java.util.List;

/**
 * Mirrors the exact JSON schema JobMatchPromptBuilder instructs Groq to
 * return. Never persisted or returned to the client until validate()
 * passes - see spec section 21.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobMatchAiResponse(
        Integer matchScore,
        List<String> matchingSkills,
        List<String> missingSkills,
        List<String> matchingExperience,
        List<String> missingRequirements,
        List<String> keywordGaps,
        List<String> recommendations
) {
    public void validate() {
        if (matchScore == null || matchScore < 0 || matchScore > 100) {
            throw new InvalidAiResponseException("AI response field 'matchScore' must be an integer between 0 and 100");
        }
        requireList(matchingSkills, "matchingSkills");
        requireList(missingSkills, "missingSkills");
        requireList(matchingExperience, "matchingExperience");
        requireList(missingRequirements, "missingRequirements");
        requireList(keywordGaps, "keywordGaps");
        requireList(recommendations, "recommendations");
    }

    private static void requireList(List<String> list, String field) {
        if (list == null) {
            throw new InvalidAiResponseException("AI response is missing required field '" + field + "'");
        }
    }
}