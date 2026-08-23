package com.interviewiq.repository;

import com.interviewiq.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    List<Resume> findByUserClerkUserIdOrderByCreatedAtDesc(String clerkUserId);
    long countByUserClerkUserId(String clerkUserId);
}