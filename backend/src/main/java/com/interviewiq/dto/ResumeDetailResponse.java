package com.interviewiq.dto;

import com.interviewiq.entity.Resume;
import com.interviewiq.entity.enums.ProcessingStatus;

import java.time.Instant;
import java.util.UUID;

public record ResumeDetailResponse(
        UUID id,
        String fileName,
        ProcessingStatus processingStatus,
        String extractedText,
        Instant createdAt,
        Instant updatedAt
) {
    public static ResumeDetailResponse from(Resume resume) {
        return new ResumeDetailResponse(
                resume.getId(),
                resume.getFileName(),
                resume.getProcessingStatus(),
                resume.getExtractedText(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }
}