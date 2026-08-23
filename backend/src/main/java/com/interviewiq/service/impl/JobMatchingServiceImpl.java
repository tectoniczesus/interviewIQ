package com.interviewiq.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.dto.ai.JobMatchAiResponse;
import com.interviewiq.entity.Job;
import com.interviewiq.entity.JobMatch;
import com.interviewiq.entity.Resume;
import com.interviewiq.entity.enums.ProcessingStatus;
import com.interviewiq.exception.InvalidAiResponseException;
import com.interviewiq.exception.ResourceNotFoundException;
import com.interviewiq.exception.UnauthorizedResourceException;
import com.interviewiq.exception.ValidationException;
import com.interviewiq.repository.JobMatchRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.GroqService;
import com.interviewiq.service.JobMatchingService;
import com.interviewiq.service.JobService;
import com.interviewiq.service.ResumeService;
import com.interviewiq.service.prompt.JobMatchPromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class JobMatchingServiceImpl implements JobMatchingService {

    private static final Logger log = LoggerFactory.getLogger(JobMatchingServiceImpl.class);
    private static final int MAX_ATTEMPTS = 2;

    private final ResumeService resumeService;
    private final JobService jobService;
    private final JobMatchRepository jobMatchRepository;
    private final GroqService groqService;
    private final JobMatchPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public JobMatchingServiceImpl(ResumeService resumeService,
                                  JobService jobService,
                                  JobMatchRepository jobMatchRepository,
                                  GroqService groqService,
                                  JobMatchPromptBuilder promptBuilder,
                                  ObjectMapper objectMapper) {
        this.resumeService = resumeService;
        this.jobService = jobService;
        this.jobMatchRepository = jobMatchRepository;
        this.groqService = groqService;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public JobMatch createMatch(UUID resumeId, UUID jobId, AuthenticatedUser authenticatedUser) {
        // Reuses ResumeService/JobService ownership checks rather than duplicating them here.
        Resume resume = resumeService.getByIdForUser(resumeId, authenticatedUser);
        Job job = jobService.getByIdForUser(jobId, authenticatedUser);

        if (resume.getProcessingStatus() != ProcessingStatus.COMPLETED
                || resume.getExtractedText() == null
                || resume.getExtractedText().isBlank()) {
            throw new ValidationException(
                    "This resume doesn't have extracted text yet - wait for processing to finish or re-upload it");
        }

        JobMatchAiResponse aiResponse = requestValidMatch(resume.getExtractedText(), job.getDescription());

        JobMatch match = new JobMatch();
        match.setResume(resume);
        match.setJob(job);
        match.setMatchScore(aiResponse.matchScore());
        match.setAnalysisJson(writeJson(aiResponse));

        return jobMatchRepository.save(match);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobMatch> listForUser(AuthenticatedUser authenticatedUser) {
        return jobMatchRepository.findByResumeUserClerkUserIdOrderByCreatedAtDesc(authenticatedUser.clerkUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public JobMatch getByIdForUser(UUID matchId, AuthenticatedUser authenticatedUser) {
        JobMatch match = jobMatchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Job match not found"));

        if (!match.getResume().getUser().getClerkUserId().equals(authenticatedUser.clerkUserId())) {
            throw new UnauthorizedResourceException("You do not have access to this job match");
        }

        return match;
    }

    private JobMatchAiResponse requestValidMatch(String resumeText, String jobDescription) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        InvalidAiResponseException lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String userPrompt = attempt == 1
                    ? promptBuilder.buildUserPrompt(resumeText, jobDescription)
                    : promptBuilder.buildRetryUserPrompt(resumeText, jobDescription);

            String raw = groqService.complete(systemPrompt, userPrompt);

            try {
                JobMatchAiResponse parsed = objectMapper.readValue(raw, JobMatchAiResponse.class);
                parsed.validate();
                return parsed;
            } catch (Exception ex) {
                log.warn("Groq job match response failed validation (attempt {}/{}): {}",
                        attempt, MAX_ATTEMPTS, ex.getMessage());
                lastError = new InvalidAiResponseException("AI response did not match the required schema", ex);
            }
        }

        throw lastError;
    }

    private String writeJson(JobMatchAiResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception ex) {
            throw new InvalidAiResponseException("Failed to serialize validated AI response", ex);
        }
    }
}