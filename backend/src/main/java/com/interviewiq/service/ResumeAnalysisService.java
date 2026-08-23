package com.interviewiq.service;

import com.interviewiq.entity.ResumeAnalysis;
import com.interviewiq.security.AuthenticatedUser;

import java.util.UUID;

public interface ResumeAnalysisService {

    /**
     * Runs a fresh Groq analysis for the given resume and persists it.
     * @throws com.interviewiq.exception.ValidationException if the resume's text hasn't finished processing
     * @throws com.interviewiq.exception.InvalidAiResponseException if Groq's response never validates, even after retry
     */
    ResumeAnalysis analyze(UUID resumeId, AuthenticatedUser authenticatedUser);

    /**
     * @throws com.interviewiq.exception.ResourceNotFoundException if this resume has never been analyzed
     */
    ResumeAnalysis getLatestForUser(UUID resumeId, AuthenticatedUser authenticatedUser);
}