package com.interviewiq.dto;

import com.interviewiq.entity.Resume;
import com.interviewiq.entity.enums.ProcessingStatus;

import java.time.Instant;
import java.util.UUID;

public record ResumeSummaryResponse(
        UUID id,
        String fileName,
        ProcessingStatus processingStatus,
        Instant createdAt
) {
    public static ResumeSummaryResponse from(Resume resume) {
        return new ResumeSummaryResponse(
                resume.getId(),
                resume.getFileName(),
                resume.getProcessingStatus(),
                resume.getCreatedAt()
        );
    }
}