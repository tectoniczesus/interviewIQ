package com.interviewiq.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.interviewiq.exception.InvalidAiResponseException;

import java.util.List;

/**
 * Groq's JSON-object response mode requires a top-level object, so the
 * question list is wrapped as {"questions": [...]} rather than a bare
 * array.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewPrepAiResponse(List<InterviewQuestionAiResponse> questions) {

    public void validate() {
        if (questions == null || questions.isEmpty()) {
            throw new InvalidAiResponseException("AI response is missing required field 'questions' or it was empty");
        }
        questions.forEach(InterviewQuestionAiResponse::validate);
    }
}