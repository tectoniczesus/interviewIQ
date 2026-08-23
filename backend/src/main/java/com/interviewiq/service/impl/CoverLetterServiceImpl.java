package com.interviewiq.service.impl;

import com.interviewiq.entity.CoverLetter;
import com.interviewiq.entity.Job;
import com.interviewiq.entity.Resume;
import com.interviewiq.entity.User;
import com.interviewiq.entity.enums.ProcessingStatus;
import com.interviewiq.exception.ResourceNotFoundException;
import com.interviewiq.exception.UnauthorizedResourceException;
import com.interviewiq.exception.ValidationException;
import com.interviewiq.repository.CoverLetterRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.CoverLetterService;
import com.interviewiq.service.GroqService;
import com.interviewiq.service.JobService;
import com.interviewiq.service.ResumeService;
import com.interviewiq.service.UserService;
import com.interviewiq.service.prompt.CoverLetterPromptBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CoverLetterServiceImpl implements CoverLetterService {

    private final ResumeService resumeService;
    private final JobService jobService;
    private final UserService userService;
    private final CoverLetterRepository coverLetterRepository;
    private final GroqService groqService;
    private final CoverLetterPromptBuilder promptBuilder;

    public CoverLetterServiceImpl(ResumeService resumeService,
                                  JobService jobService,
                                  UserService userService,
                                  CoverLetterRepository coverLetterRepository,
                                  GroqService groqService,
                                  CoverLetterPromptBuilder promptBuilder) {
        this.resumeService = resumeService;
        this.jobService = jobService;
        this.userService = userService;
        this.coverLetterRepository = coverLetterRepository;
        this.groqService = groqService;
        this.promptBuilder = promptBuilder;
    }

    @Override
    @Transactional
    public CoverLetter create(UUID resumeId, UUID jobId, String candidateName, AuthenticatedUser authenticatedUser) {
        Resume resume = resumeService.getByIdForUser(resumeId, authenticatedUser);
        Job job = jobService.getByIdForUser(jobId, authenticatedUser);
        requireExtractedText(resume);

        User user = userService.findOrCreate(authenticatedUser);

        String content = generateContent(resume, job, candidateName);

        CoverLetter coverLetter = new CoverLetter();
        coverLetter.setUser(user);
        coverLetter.setResume(resume);
        coverLetter.setJob(job);
        coverLetter.setContent(content);

        return coverLetterRepository.save(coverLetter);
    }

    @Override
    @Transactional
    public CoverLetter regenerate(UUID coverLetterId, AuthenticatedUser authenticatedUser) {
        CoverLetter coverLetter = getByIdForUser(coverLetterId, authenticatedUser);
        requireExtractedText(coverLetter.getResume());

        String content = generateContent(coverLetter.getResume(), coverLetter.getJob(), null);
        coverLetter.setContent(content);

        return coverLetterRepository.save(coverLetter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoverLetter> listForUser(AuthenticatedUser authenticatedUser) {
        return coverLetterRepository.findByUserClerkUserIdOrderByCreatedAtDesc(authenticatedUser.clerkUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public CoverLetter getByIdForUser(UUID coverLetterId, AuthenticatedUser authenticatedUser) {
        CoverLetter coverLetter = coverLetterRepository.findById(coverLetterId)
                .orElseThrow(() -> new ResourceNotFoundException("Cover letter not found"));

        if (!coverLetter.getUser().getClerkUserId().equals(authenticatedUser.clerkUserId())) {
            throw new UnauthorizedResourceException("You do not have access to this cover letter");
        }

        return coverLetter;
    }

    @Override
    @Transactional
    public void deleteForUser(UUID coverLetterId, AuthenticatedUser authenticatedUser) {
        CoverLetter coverLetter = getByIdForUser(coverLetterId, authenticatedUser);
        coverLetterRepository.delete(coverLetter);
    }

    private String generateContent(Resume resume, Job job, String candidateName) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(
                resume.getExtractedText(), job.getDescription(), job.getTitle(), job.getCompany(), candidateName);

        return groqService.completeText(systemPrompt, userPrompt).trim();
    }

    private void requireExtractedText(Resume resume) {
        if (resume.getProcessingStatus() != ProcessingStatus.COMPLETED
                || resume.getExtractedText() == null
                || resume.getExtractedText().isBlank()) {
            throw new ValidationException(
                    "This resume doesn't have extracted text yet - wait for processing to finish or re-upload it");
        }
    }
}