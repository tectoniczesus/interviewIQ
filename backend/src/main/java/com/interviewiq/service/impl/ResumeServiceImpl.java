package com.interviewiq.service.impl;

import com.interviewiq.entity.Resume;
import com.interviewiq.entity.User;
import com.interviewiq.entity.enums.ProcessingStatus;
import com.interviewiq.exception.ResourceNotFoundException;
import com.interviewiq.exception.UnauthorizedResourceException;
import com.interviewiq.repository.CoverLetterRepository;
import com.interviewiq.repository.InterviewQuestionRepository;
import com.interviewiq.repository.InterviewSessionRepository;
import com.interviewiq.repository.JobMatchRepository;
import com.interviewiq.repository.ResumeAnalysisRepository;
import com.interviewiq.repository.ResumeRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.PdfExtractionService;
import com.interviewiq.service.ResumeFileValidator;
import com.interviewiq.service.ResumeService;
import com.interviewiq.service.SupabaseStorageService;
import com.interviewiq.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class ResumeServiceImpl implements ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeServiceImpl.class);

    private final ResumeRepository resumeRepository;
    private final ResumeFileValidator resumeFileValidator;
    private final SupabaseStorageService storageService;
    private final PdfExtractionService pdfExtractionService;
    private final UserService userService;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final JobMatchRepository jobMatchRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;

    public ResumeServiceImpl(ResumeRepository resumeRepository,
                             ResumeFileValidator resumeFileValidator,
                             SupabaseStorageService storageService,
                             PdfExtractionService pdfExtractionService,
                             UserService userService,
                             ResumeAnalysisRepository resumeAnalysisRepository,
                             JobMatchRepository jobMatchRepository,
                             CoverLetterRepository coverLetterRepository,
                             InterviewSessionRepository interviewSessionRepository,
                             InterviewQuestionRepository interviewQuestionRepository) {
        this.resumeRepository = resumeRepository;
        this.resumeFileValidator = resumeFileValidator;
        this.storageService = storageService;
        this.pdfExtractionService = pdfExtractionService;
        this.userService = userService;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.coverLetterRepository = coverLetterRepository;
        this.interviewSessionRepository = interviewSessionRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
    }

    @Override
    @Transactional
    public Resume upload(MultipartFile file, AuthenticatedUser authenticatedUser) {
        resumeFileValidator.validate(file);

        User user = userService.findOrCreate(authenticatedUser);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception ex) {
            throw new com.interviewiq.exception.InvalidFileException("Could not read the uploaded file");
        }

        String storagePath = storageService.upload(
                "resumes/" + user.getId(), file.getOriginalFilename(), bytes, file.getContentType());

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setFileName(file.getOriginalFilename());
        resume.setStoragePath(storagePath);
        resume.setProcessingStatus(ProcessingStatus.PROCESSING);
        resume = resumeRepository.save(resume);

        // Text extraction happens synchronously for now (see ARCHITECTURE.md -
        // async processing is a documented gap this is structured to allow
        // adding later without a redesign).
        try {
            String extractedText = pdfExtractionService.extractText(bytes);
            resume.setExtractedText(extractedText);
            resume.setProcessingStatus(ProcessingStatus.COMPLETED);
        } catch (Exception ex) {
            log.warn("Text extraction failed for resume {}: {}", resume.getId(), ex.getMessage());
            resume.setProcessingStatus(ProcessingStatus.FAILED);
        }

        return resumeRepository.save(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resume> listForUser(AuthenticatedUser authenticatedUser) {
        return resumeRepository.findByUserClerkUserIdOrderByCreatedAtDesc(authenticatedUser.clerkUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public Resume getByIdForUser(UUID resumeId, AuthenticatedUser authenticatedUser) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        assertOwnership(resume, authenticatedUser);
        return resume;
    }

    @Override
    @Transactional
    public void deleteForUser(UUID resumeId, AuthenticatedUser authenticatedUser) {
        Resume resume = getByIdForUser(resumeId, authenticatedUser);

        // Delete every record that has a required (non-nullable) foreign key to
        // this resume, deepest dependents first, or Postgres rejects the final
        // delete with a constraint violation (spec business rule #9).
        interviewSessionRepository.findByResumeId(resumeId).forEach(session -> {
            interviewQuestionRepository.deleteAll(interviewQuestionRepository.findBySessionId(session.getId()));
        });
        interviewSessionRepository.deleteAll(interviewSessionRepository.findByResumeId(resumeId));
        coverLetterRepository.deleteAll(coverLetterRepository.findByResumeId(resumeId));
        jobMatchRepository.deleteAll(jobMatchRepository.findByResumeIdOrderByCreatedAtDesc(resumeId));
        resumeAnalysisRepository.deleteAll(resumeAnalysisRepository.findByResumeIdOrderByCreatedAtDesc(resumeId));

        try {
            storageService.delete(resume.getStoragePath());
        } catch (Exception ex) {
            // Don't let a storage hiccup corrupt DB state (spec business rule #10) -
            // log it and still remove the DB record; an orphaned storage object is
            // recoverable, an inconsistent DB row is more annoying to fix.
            log.warn("Failed to delete storage object for resume {}: {}", resumeId, ex.getMessage());
        }

        resumeRepository.delete(resume);
    }

    private void assertOwnership(Resume resume, AuthenticatedUser authenticatedUser) {
        if (!resume.getUser().getClerkUserId().equals(authenticatedUser.clerkUserId())) {
            throw new UnauthorizedResourceException("You do not have access to this resume");
        }
    }
}