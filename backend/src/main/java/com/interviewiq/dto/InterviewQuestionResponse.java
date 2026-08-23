package com.interviewiq.dto;

import com.interviewiq.entity.InterviewQuestion;
import com.interviewiq.entity.enums.QuestionCategory;
import com.interviewiq.entity.enums.QuestionDifficulty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterviewQuestionResponse(
        UUID id,
        QuestionCategory category,
        QuestionDifficulty difficulty,
        String question,
        String suggestedAnswer,
        List<String> keyPoints,
        Instant createdAt
) {
    public static InterviewQuestionResponse from(InterviewQuestion question) {
        return new InterviewQuestionResponse(
                question.getId(),
                question.getCategory(),
                question.getDifficulty(),
                question.getQuestion(),
                question.getSuggestedAnswer(),
                question.getKeyPoints(),
                question.getCreatedAt()
        );
    }
}