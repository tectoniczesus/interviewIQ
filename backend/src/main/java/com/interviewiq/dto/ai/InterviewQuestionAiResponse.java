package com.interviewiq.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.interviewiq.exception.InvalidAiResponseException;

import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewQuestionAiResponse(
        String question,
        String category,
        String difficulty,
        String suggestedAnswer,
        List<String> keyPoints
) {
    private static final Set<String> VALID_CATEGORIES = Set.of("TECHNICAL", "HR", "RESUME_BASED", "JOB_SPECIFIC");
    private static final Set<String> VALID_DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    public void validate() {
        if (question == null || question.isBlank()) {
            throw new InvalidAiResponseException("Interview question is missing required field 'question'");
        }
        if (category == null || !VALID_CATEGORIES.contains(category)) {
            throw new InvalidAiResponseException(
                    "Interview question field 'category' must be one of " + VALID_CATEGORIES + " but was '" + category + "'");
        }
        if (difficulty == null || !VALID_DIFFICULTIES.contains(difficulty)) {
            throw new InvalidAiResponseException(
                    "Interview question field 'difficulty' must be one of " + VALID_DIFFICULTIES + " but was '" + difficulty + "'");
        }
        if (suggestedAnswer == null || suggestedAnswer.isBlank()) {
            throw new InvalidAiResponseException("Interview question is missing required field 'suggestedAnswer'");
        }
        if (keyPoints == null) {
            throw new InvalidAiResponseException("Interview question is missing required field 'keyPoints'");
        }
    }
}