package com.interviewiq.service;

/**
 * Wraps the Supabase Storage REST API. This is the only place in the
 * backend allowed to talk to Supabase Storage directly - resume PDFs are
 * never stored in PostgreSQL itself, only their storage path.
 */
public interface SupabaseStorageService {

    /**
     * Uploads a file and returns the storage path it was saved at
     * (e.g. "resumes/{userId}/{uuid}-filename.pdf").
     */
    String upload(String pathPrefix, String originalFileName, byte[] content, String contentType);

    byte[] download(String storagePath);

    void delete(String storagePath);
}
