package com.interviewiq.service;

import com.interviewiq.entity.Job;
import com.interviewiq.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

public interface JobService {

    Job createFromDescription(String title, String company, String description, AuthenticatedUser authenticatedUser);

    /**
     * @throws com.interviewiq.exception.ValidationException if the URL is malformed
     * @throws com.interviewiq.exception.FirecrawlApiException if extraction fails or returns empty content
     */
    Job createFromUrl(String title, String company, String jobUrl, AuthenticatedUser authenticatedUser);

    List<Job> listForUser(AuthenticatedUser authenticatedUser);

    Job getByIdForUser(UUID jobId, AuthenticatedUser authenticatedUser);

    void deleteForUser(UUID jobId, AuthenticatedUser authenticatedUser);
}