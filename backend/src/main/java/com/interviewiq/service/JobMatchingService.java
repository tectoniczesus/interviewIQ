package com.interviewiq.service;

import com.interviewiq.entity.JobMatch;
import com.interviewiq.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

public interface JobMatchingService {

    /**
     * Runs a fresh Groq compatibility analysis between a resume and a job,
     * persists it. Both resumeId and jobId must belong to the caller.
     * @throws com.interviewiq.exception.ValidationException if the resume's text hasn't finished processing
     */
    JobMatch createMatch(UUID resumeId, UUID jobId, AuthenticatedUser authenticatedUser);

    List<JobMatch> listForUser(AuthenticatedUser authenticatedUser);

    JobMatch getByIdForUser(UUID matchId, AuthenticatedUser authenticatedUser);
}