package com.interviewiq.repository;

import com.interviewiq.entity.LearningRoadmap;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * open-in-view is disabled, so every read that is later mapped to a DTO
 * eagerly loads jobMatch -> job / resume -> user via an entity graph.
 */
public interface LearningRoadmapRepository extends JpaRepository<LearningRoadmap, UUID> {

    @EntityGraph(attributePaths = {"jobMatch", "jobMatch.job", "jobMatch.resume", "jobMatch.resume.user"})
    Optional<LearningRoadmap> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"jobMatch", "jobMatch.job", "jobMatch.resume", "jobMatch.resume.user"})
    Optional<LearningRoadmap> findByJobMatchId(UUID jobMatchId);

    @EntityGraph(attributePaths = {"jobMatch", "jobMatch.job", "jobMatch.resume"})
    List<LearningRoadmap> findByJobMatchResumeUserClerkUserIdOrderByUpdatedAtDesc(String clerkUserId);

    /** Row lock so two quick task toggles on the same roadmap can't overwrite each other's progress. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LearningRoadmap r where r.id = :id")
    Optional<LearningRoadmap> findByIdForUpdate(@Param("id") UUID id);

    List<LearningRoadmap> findByJobMatchResumeId(UUID resumeId);

    List<LearningRoadmap> findByJobMatchJobId(UUID jobId);

    long countByJobMatchResumeUserClerkUserId(String clerkUserId);
}
