package com.interviewiq.repository;

import com.interviewiq.entity.CoverLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CoverLetterRepository extends JpaRepository<CoverLetter, UUID> {
    List<CoverLetter> findByUserClerkUserIdOrderByCreatedAtDesc(String clerkUserId);
    long countByUserClerkUserId(String clerkUserId);
    List<CoverLetter> findByResumeId(UUID resumeId);
    List<CoverLetter> findByJobId(UUID jobId);
}