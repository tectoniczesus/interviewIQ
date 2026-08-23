package com.interviewiq.repository;

import com.interviewiq.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {
    List<InterviewSession> findByUserClerkUserIdOrderByCreatedAtDesc(String clerkUserId);
    long countByUserClerkUserId(String clerkUserId);
    List<InterviewSession> findByResumeId(UUID resumeId);
    List<InterviewSession> findByJobId(UUID jobId);
}