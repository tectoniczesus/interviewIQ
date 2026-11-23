package com.interviewiq.service;

import com.interviewiq.entity.LearningRoadmap;
import com.interviewiq.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

public interface LearningRoadmapService {

    int DEFAULT_HOURS_PER_WEEK = 8;

    /**
     * Generates (or regenerates, replacing the plan and resetting progress) the
     * learning roadmap for a job match the caller owns.
     */
    LearningRoadmap generateForMatch(UUID jobMatchId, Integer hoursPerWeek, AuthenticatedUser authenticatedUser);

    /** @throws com.interviewiq.exception.ResourceNotFoundException if no roadmap exists for this match yet */
    LearningRoadmap getForMatch(UUID jobMatchId, AuthenticatedUser authenticatedUser);

    LearningRoadmap getByIdForUser(UUID roadmapId, AuthenticatedUser authenticatedUser);

    List<LearningRoadmap> listForUser(AuthenticatedUser authenticatedUser);

    /** @throws com.interviewiq.exception.ValidationException if taskId isn't part of this roadmap */
    LearningRoadmap setTaskCompleted(UUID roadmapId, String taskId, boolean completed, AuthenticatedUser authenticatedUser);

    void deleteForUser(UUID roadmapId, AuthenticatedUser authenticatedUser);
}
