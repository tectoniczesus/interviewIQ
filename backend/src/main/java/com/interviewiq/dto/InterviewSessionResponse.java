package com.interviewiq.dto;

import com.interviewiq.entity.InterviewSession;

import java.time.Instant;
import java.util.UUID;

public record InterviewSessionResponse(
        UUID id,
        UUID resumeId,
        UUID jobId,
        String role,
        Instant createdAt
) {
    public static InterviewSessionResponse from(InterviewSession session) {
        return new InterviewSessionResponse(
                session.getId(),
                session.getResume().getId(),
                session.getJob().getId(),
                session.getRole(),
                session.getCreatedAt()
        );
    }
}