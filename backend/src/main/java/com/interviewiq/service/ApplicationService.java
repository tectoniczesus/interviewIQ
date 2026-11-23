package com.interviewiq.service;

import com.interviewiq.dto.ApplicationEventResponse;
import com.interviewiq.dto.ApplicationResponse;
import com.interviewiq.dto.ApplicationUpdateRequest;
import com.interviewiq.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

/**
 * Application tracker: the pipeline stage, dates and notes stored on each Job.
 * Ownership is enforced through {@link JobService#getByIdForUser}.
 */
public interface ApplicationService {

    List<ApplicationResponse> listForUser(AuthenticatedUser authenticatedUser);

    ApplicationResponse getForUser(UUID jobId, AuthenticatedUser authenticatedUser);

    /**
     * Replaces the tracker fields. A stage change is recorded as an event and,
     * when moving to APPLIED, fills in a missing applied date (today) and
     * follow-up date (applied date + 7 days).
     */
    ApplicationResponse update(UUID jobId, ApplicationUpdateRequest request, AuthenticatedUser authenticatedUser);

    List<ApplicationEventResponse> listEvents(UUID jobId, AuthenticatedUser authenticatedUser);
}
