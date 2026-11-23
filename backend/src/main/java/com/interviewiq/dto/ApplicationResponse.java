package com.interviewiq.dto;

import com.interviewiq.entity.Job;
import com.interviewiq.entity.enums.ApplicationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A job as it appears on the application tracker board. */
public record ApplicationResponse(
        UUID jobId,
        String title,
        String company,
        String jobUrl,
        ApplicationStatus status,
        LocalDate appliedOn,
        LocalDate followUpOn,
        String notes,
        Instant statusChangedAt,
        Instant createdAt,
        Integer latestMatchScore
) {
    public static ApplicationResponse from(Job job, Integer latestMatchScore) {
        return new ApplicationResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getJobUrl(),
                job.getApplicationStatus(),
                job.getAppliedOn(),
                job.getFollowUpOn(),
                job.getApplicationNotes(),
                job.getStatusChangedAt(),
                job.getCreatedAt(),
                latestMatchScore
        );
    }
}
