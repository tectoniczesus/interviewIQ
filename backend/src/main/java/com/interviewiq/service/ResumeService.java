package com.interviewiq.service;

import com.interviewiq.entity.Resume;
import com.interviewiq.security.AuthenticatedUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ResumeService {

    /**
     * Validates, stores (Supabase Storage), and text-extracts (PDFBox) a
     * new resume upload, then persists it. Never trusts a userId from the
     * caller - always scoped to the authenticated user.
     */
    Resume upload(MultipartFile file, AuthenticatedUser authenticatedUser);

    List<Resume> listForUser(AuthenticatedUser authenticatedUser);

    /**
     * @throws com.interviewiq.exception.ResourceNotFoundException if no resume with this id exists
     * @throws com.interviewiq.exception.UnauthorizedResourceException if it belongs to a different user
     */
    Resume getByIdForUser(UUID resumeId, AuthenticatedUser authenticatedUser);

    void deleteForUser(UUID resumeId, AuthenticatedUser authenticatedUser);
}