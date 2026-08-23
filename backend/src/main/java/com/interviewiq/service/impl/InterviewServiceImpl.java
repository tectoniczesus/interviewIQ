package com.interviewiq.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.dto.ai.InterviewPrepAiResponse;
import com.interviewiq.dto.ai.InterviewQuestionAiResponse;
import com.interviewiq.entity.Job;
import com.interviewiq.entity.InterviewQuestion;
import com.interviewiq.entity.InterviewSession;
import com.interviewiq.entity.Resume;
import com.interviewiq.entity.User;
import com.interviewiq.entity.enums.ProcessingStatus;
import com.interviewiq.entity.enums.QuestionCategory;
import com.interviewiq.entity.enums.QuestionDifficulty;
import com.interviewiq.exception.InvalidAiResponseException;
import com.interviewiq.exception.ResourceNotFoundException;
import com.interviewiq.exception.UnauthorizedResourceException;
import com.interviewiq.exception.ValidationException;
import com.interviewiq.repository.InterviewQuestionRepository;
import com.interviewiq.repository.InterviewSessionRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.GroqService;
import com.interviewiq.service.InterviewService;
import com.interviewiq.service.JobService;
import com.interviewiq.service.ResumeService;
import com.interviewiq.service.UserService;
import com.interviewiq.service.prompt.InterviewPrepPromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InterviewServiceImpl implements InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewServiceImpl.class);
    private static final int MAX_ATTEMPTS = 2;

    private final ResumeService resumeService;
    private final JobService jobService;
    private final UserService userService;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final GroqService groqService;
    private final InterviewPrepPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public InterviewServiceImpl(ResumeService resumeService,
                                JobService jobService,
                                UserService userService,
                                InterviewSessionRepository sessionRepository,
                                InterviewQuestionRepository questionRepository,
                                GroqService groqService,
                                InterviewPrepPromptBuilder promptBuilder,
                                ObjectMapper objectMapper) {
        this.resumeService = resumeService;
        this.jobService = jobService;
        this.userService = userService;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.groqService = groqService;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public InterviewSession create(UUID resumeId, UUID jobId, String role, AuthenticatedUser authenticatedUser) {
        if (role == null || role.isBlank()) {
            throw new ValidationException("Please provide a target role");
        }

        Resume resume = resumeService.getByIdForUser(resumeId, authenticatedUser);
        Job job = jobService.getByIdForUser(jobId, authenticatedUser);
        requireExtractedText(resume);

        User user = userService.findOrCreate(authenticatedUser);

        InterviewSession session = new InterviewSession();
        session.setUser(user);
        session.setResume(resume);
        session.setJob(job);
        session.setRole(role.trim());
        session = sessionRepository.save(session);

        List<InterviewQuestionAiResponse> generated =
                requestValidQuestions(resume.getExtractedText(), job.getDescription(), session.getRole());
        questionRepository.saveAll(toEntities(generated, session));

        return session;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewSession> listForUser(AuthenticatedUser authenticatedUser) {
        return sessionRepository.findByUserClerkUserIdOrderByCreatedAtDesc(authenticatedUser.clerkUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewSession getByIdForUser(UUID sessionId, AuthenticatedUser authenticatedUser) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview session not found"));

        assertOwnership(session, authenticatedUser);
        return session;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewQuestion> getQuestionsForUser(UUID sessionId, QuestionCategory category,
                                                       AuthenticatedUser authenticatedUser) {
        getByIdForUser(sessionId, authenticatedUser); // ownership check

        return category == null
                ? questionRepository.findBySessionId(sessionId)
                : questionRepository.findBySessionIdAndCategory(sessionId, category);
    }

    @Override
    @Transactional
    public List<InterviewQuestion> regenerate(UUID sessionId, AuthenticatedUser authenticatedUser) {
        InterviewSession session = getByIdForUser(sessionId, authenticatedUser);
        requireExtractedText(session.getResume());

        List<InterviewQuestion> existing = questionRepository.findBySessionId(sessionId);
        questionRepository.deleteAll(existing);

        List<InterviewQuestionAiResponse> generated = requestValidQuestions(
                session.getResume().getExtractedText(), session.getJob().getDescription(), session.getRole());

        return questionRepository.saveAll(toEntities(generated, session));
    }

    @Override
    @Transactional
    public void deleteForUser(UUID sessionId, AuthenticatedUser authenticatedUser) {
        InterviewSession session = getByIdForUser(sessionId, authenticatedUser);

        questionRepository.deleteAll(questionRepository.findBySessionId(sessionId));
        sessionRepository.delete(session);
    }

    private List<InterviewQuestionAiResponse> requestValidQuestions(String resumeText, String jobDescription, String role) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        InvalidAiResponseException lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String userPrompt = attempt == 1
                    ? promptBuilder.buildUserPrompt(resumeText, jobDescription, role)
                    : promptBuilder.buildRetryUserPrompt(resumeText, jobDescription, role);

            String raw = groqService.complete(systemPrompt, userPrompt);

            try {
                InterviewPrepAiResponse parsed = objectMapper.readValue(raw, InterviewPrepAiResponse.class);
                parsed.validate();
                return parsed.questions();
            } catch (Exception ex) {
                log.warn("Groq interview prep response failed validation (attempt {}/{}): {}",
                        attempt, MAX_ATTEMPTS, ex.getMessage());
                lastError = new InvalidAiResponseException("AI response did not match the required schema", ex);
            }
        }

        throw lastError;
    }

    private List<InterviewQuestion> toEntities(List<InterviewQuestionAiResponse> aiQuestions, InterviewSession session) {
        return aiQuestions.stream().map(ai -> {
            InterviewQuestion question = new InterviewQuestion();
            question.setSession(session);
            question.setCategory(QuestionCategory.valueOf(ai.category()));
            question.setDifficulty(QuestionDifficulty.valueOf(ai.difficulty()));
            question.setQuestion(ai.question());
            question.setSuggestedAnswer(ai.suggestedAnswer());
            question.setKeyPoints(ai.keyPoints());
            return question;
        }).toList();
    }

    private void requireExtractedText(Resume resume) {
        if (resume.getProcessingStatus() != ProcessingStatus.COMPLETED
                || resume.getExtractedText() == null
                || resume.getExtractedText().isBlank()) {
            throw new ValidationException(
                    "This resume doesn't have extracted text yet - wait for processing to finish or re-upload it");
        }
    }

    private void assertOwnership(InterviewSession session, AuthenticatedUser authenticatedUser) {
        if (!session.getUser().getClerkUserId().equals(authenticatedUser.clerkUserId())) {
            throw new UnauthorizedResourceException("You do not have access to this interview session");
        }
    }
}