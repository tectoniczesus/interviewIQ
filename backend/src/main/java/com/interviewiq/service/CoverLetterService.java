package com.interviewiq.service;

import com.interviewiq.entity.CoverLetter;
import com.interviewiq.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

public interface CoverLetterService {

    CoverLetter create(UUID resumeId, UUID jobId, String candidateName, AuthenticatedUser authenticatedUser);

    /**
     * Regenerates the content of an existing cover letter in place (same
     * resume/job pair, fresh Groq call), rather than creating a new record.
     */
    CoverLetter regenerate(UUID coverLetterId, AuthenticatedUser authenticatedUser);

    List<CoverLetter> listForUser(AuthenticatedUser authenticatedUser);

    CoverLetter getByIdForUser(UUID coverLetterId, AuthenticatedUser authenticatedUser);

    void deleteForUser(UUID coverLetterId, AuthenticatedUser authenticatedUser);
}