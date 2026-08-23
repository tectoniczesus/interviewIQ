package com.interviewiq.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.dto.ai.ResumeAnalysisAiResponse;
import com.interviewiq.entity.Resume;
import com.interviewiq.entity.ResumeAnalysis;
import com.interviewiq.entity.enums.ProcessingStatus;
import com.interviewiq.exception.InvalidAiResponseException;
import com.interviewiq.exception.ResourceNotFoundException;
import com.interviewiq.exception.ValidationException;
import com.interviewiq.repository.ResumeAnalysisRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.GroqService;
import com.interviewiq.service.ResumeAnalysisService;
import com.interviewiq.service.ResumeService;
import com.interviewiq.service.prompt.ResumeAnalysisPromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ResumeAnalysisServiceImpl implements ResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisServiceImpl.class);
    private static final int MAX_ATTEMPTS = 2;

    private final ResumeService resumeService;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final GroqService groqService;
    private final ResumeAnalysisPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public ResumeAnalysisServiceImpl(ResumeService resumeService,
                                     ResumeAnalysisRepository resumeAnalysisRepository,
                                     GroqService groqService,
                                     ResumeAnalysisPromptBuilder promptBuilder,
                                     ObjectMapper objectMapper) {
        this.resumeService = resumeService;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.groqService = groqService;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ResumeAnalysis analyze(UUID resumeId, AuthenticatedUser authenticatedUser) {
        // Reuses ResumeService's ownership check rather than duplicating it here.
        Resume resume = resumeService.getByIdForUser(resumeId, authenticatedUser);

        if (resume.getProcessingStatus() != ProcessingStatus.COMPLETED
                || resume.getExtractedText() == null
                || resume.getExtractedText().isBlank()) {
            throw new ValidationException(
                    "This resume doesn't have extracted text yet - wait for processing to finish or re-upload it");
        }

        ResumeAnalysisAiResponse aiResponse = requestValidAnalysis(resume.getExtractedText());

        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setResume(resume);
        analysis.setAtsScore(aiResponse.atsScore());
        analysis.setOverallScore(aiResponse.overallScore());
        analysis.setAnalysisJson(writeJson(aiResponse));

        return resumeAnalysisRepository.save(analysis);
    }

    @Override
    public ResumeAnalysis getLatestForUser(UUID resumeId, AuthenticatedUser authenticatedUser) {
        // Ownership check, even though we don't need the Resume itself here.
        resumeService.getByIdForUser(resumeId, authenticatedUser);

        return resumeAnalysisRepository.findByResumeIdOrderByCreatedAtDesc(resumeId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No analysis found for this resume yet"));
    }

    private ResumeAnalysisAiResponse requestValidAnalysis(String resumeText) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        InvalidAiResponseException lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String userPrompt = attempt == 1
                    ? promptBuilder.buildUserPrompt(resumeText)
                    : promptBuilder.buildRetryUserPrompt(resumeText);

            String raw = groqService.complete(systemPrompt, userPrompt);

            try {
                ResumeAnalysisAiResponse parsed = objectMapper.readValue(raw, ResumeAnalysisAiResponse.class);
                parsed.validate();
                return parsed;
            } catch (Exception ex) {
                log.warn("Groq resume analysis response failed validation (attempt {}/{}): {}",
                        attempt, MAX_ATTEMPTS, ex.getMessage());
                lastError = new InvalidAiResponseException("AI response did not match the required schema", ex);
            }
        }

        throw lastError;
    }

    private String writeJson(ResumeAnalysisAiResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception ex) {
            throw new InvalidAiResponseException("Failed to serialize validated AI response", ex);
        }
    }
}