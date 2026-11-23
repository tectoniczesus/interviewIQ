package com.interviewiq.dto;

import com.interviewiq.entity.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Full replacement of a job's tracker fields (the client always sends the
 * whole state, so a null date means "cleared", not "unchanged").
 */
public record ApplicationUpdateRequest(
        @NotNull(message = "status is required")
        ApplicationStatus status,
        LocalDate appliedOn,
        LocalDate followUpOn,
        @Size(max = 5000, message = "notes must be 5000 characters or fewer")
        String notes
) {
}
