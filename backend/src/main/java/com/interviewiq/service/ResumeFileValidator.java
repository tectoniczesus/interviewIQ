package com.interviewiq.service;

import com.interviewiq.config.UploadConfig;
import com.interviewiq.exception.InvalidFileException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ResumeFileValidator {

    private final UploadConfig uploadConfig;

    public ResumeFileValidator(UploadConfig uploadConfig) {
        this.uploadConfig = uploadConfig;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Please choose a resume file to upload");
        }

        if (file.getSize() > uploadConfig.getMaxSizeBytes()) {
            double maxMb = uploadConfig.getMaxSizeBytes() / (1024.0 * 1024.0);
            throw new InvalidFileException("File is too large - maximum size is %.0fMB".formatted(maxMb));
        }

        String contentType = file.getContentType();
        boolean typeAllowed = contentType != null && uploadConfig.getAllowedContentTypesList().contains(contentType);
        boolean nameLooksLikePdf = file.getOriginalFilename() != null
                && file.getOriginalFilename().toLowerCase().endsWith(".pdf");

        if (!typeAllowed && !nameLooksLikePdf) {
            throw new InvalidFileException("Only PDF resumes are supported right now");
        }
    }
}