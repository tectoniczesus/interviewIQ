package com.interviewiq.dto;

import com.interviewiq.entity.Job;
import com.interviewiq.entity.enums.JobSource;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String title,
        String company,
        String jobUrl,
        String description,
        JobSource source,
        Instant createdAt
) {
    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getJobUrl(),
                job.getDescription(),
                job.getSource(),
                job.getCreatedAt()
        );
    }
}