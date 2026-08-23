package com.interviewiq.repository;

import com.interviewiq.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobMatchRepository extends JpaRepository<JobMatch, UUID> {
    List<JobMatch> findByResumeIdOrderByCreatedAtDesc(UUID resumeId);
    List<JobMatch> findByJobIdOrderByCreatedAtDesc(UUID jobId);
    List<JobMatch> findByResumeUserClerkUserIdOrderByCreatedAtDesc(String clerkUserId);
    long countByResumeUserClerkUserId(String clerkUserId);
}