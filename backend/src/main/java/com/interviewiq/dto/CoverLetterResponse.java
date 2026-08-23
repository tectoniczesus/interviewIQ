package com.interviewiq.dto;

import com.interviewiq.entity.CoverLetter;

import java.time.Instant;
import java.util.UUID;

public record CoverLetterResponse(
        UUID id,
        UUID resumeId,
        UUID jobId,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
    public static CoverLetterResponse from(CoverLetter coverLetter) {
        return new CoverLetterResponse(
                coverLetter.getId(),
                coverLetter.getResume().getId(),
                coverLetter.getJob().getId(),
                coverLetter.getContent(),
                coverLetter.getCreatedAt(),
                coverLetter.getUpdatedAt()
        );
    }
}