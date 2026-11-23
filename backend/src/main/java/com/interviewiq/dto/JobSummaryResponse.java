package com.interviewiq.dto;

import com.interviewiq.entity.Job;
import com.interviewiq.entity.enums.ApplicationStatus;
import com.interviewiq.entity.enums.JobSource;

import java.time.Instant;
import java.util.UUID;

public record JobSummaryResponse(
        UUID id,
        String title,
        String company,
        JobSource source,
        String descriptionPreview,
        ApplicationStatus applicationStatus,
        Instant createdAt
) {
    public static JobSummaryResponse from(Job job) {
        String description = job.getDescription() == null ? "" : job.getDescription();
        String preview = description.length() > 200 ? description.substring(0, 200) + "…" : description;

        return new JobSummaryResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getSource(),
                preview,
                job.getApplicationStatus(),
                job.getCreatedAt()
        );
    }
}