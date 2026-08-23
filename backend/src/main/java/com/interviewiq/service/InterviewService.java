package com.interviewiq.service;

import com.interviewiq.entity.InterviewQuestion;
import com.interviewiq.entity.InterviewSession;
import com.interviewiq.entity.enums.QuestionCategory;
import com.interviewiq.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

public interface InterviewService {

    /**
     * Creates a session and generates + persists its full question set
     * (3 questions per category) in one call.
     * @throws com.interviewiq.exception.ValidationException if the resume's text hasn't finished processing
     */
    InterviewSession create(UUID resumeId, UUID jobId, String role, AuthenticatedUser authenticatedUser);

    List<InterviewSession> listForUser(AuthenticatedUser authenticatedUser);

    InterviewSession getByIdForUser(UUID sessionId, AuthenticatedUser authenticatedUser);

    /**
     * @param category optional filter; null returns all categories
     */
    List<InterviewQuestion> getQuestionsForUser(UUID sessionId, QuestionCategory category, AuthenticatedUser authenticatedUser);

    /**
     * Regenerates the full question set for an existing session (replaces
     * the old questions with a fresh Groq call).
     */
    List<InterviewQuestion> regenerate(UUID sessionId, AuthenticatedUser authenticatedUser);

    void deleteForUser(UUID sessionId, AuthenticatedUser authenticatedUser);
}