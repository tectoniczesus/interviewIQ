package com.interviewiq.dto;

import com.interviewiq.entity.ApplicationEvent;
import com.interviewiq.entity.enums.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationEventResponse(
        UUID id,
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        Instant createdAt
) {
    public static ApplicationEventResponse from(ApplicationEvent event) {
        return new ApplicationEventResponse(event.getId(), event.getFromStatus(), event.getToStatus(), event.getCreatedAt());
    }
}
