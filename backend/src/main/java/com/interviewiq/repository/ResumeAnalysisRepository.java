package com.interviewiq.repository;

import com.interviewiq.entity.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, UUID> {
    List<ResumeAnalysis> findByResumeIdOrderByCreatedAtDesc(UUID resumeId);
    Optional<ResumeAnalysis> findFirstByResumeIdOrderByCreatedAtDesc(UUID resumeId);
    Optional<ResumeAnalysis> findFirstByResumeUserClerkUserIdOrderByCreatedAtDesc(String clerkUserId);
}