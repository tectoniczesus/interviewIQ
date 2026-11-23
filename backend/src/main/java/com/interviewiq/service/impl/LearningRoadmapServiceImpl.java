package com.interviewiq.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.dto.RoadmapContent;
import com.interviewiq.dto.ai.JobMatchAiResponse;
import com.interviewiq.dto.ai.LearningRoadmapAiResponse;
import com.interviewiq.entity.Job;
import com.interviewiq.entity.JobMatch;
import com.interviewiq.entity.LearningRoadmap;
import com.interviewiq.exception.InvalidAiResponseException;
import com.interviewiq.exception.ResourceNotFoundException;
import com.interviewiq.exception.UnauthorizedResourceException;
import com.interviewiq.exception.ValidationException;
import com.interviewiq.repository.LearningRoadmapRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.GroqService;
import com.interviewiq.service.JobMatchingService;
import com.interviewiq.service.LearningRoadmapService;
import com.interviewiq.service.prompt.LearningRoadmapPromptBuilder;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LearningRoadmapServiceImpl implements LearningRoadmapService {

    private static final Logger log = LoggerFactory.getLogger(LearningRoadmapServiceImpl.class);
    static final int MAX_ATTEMPTS = 2;
    static final int MIN_HOURS_PER_WEEK = 2;
    static final int MAX_HOURS_PER_WEEK = 40;
    private static final int MAX_ROLE_LENGTH = 255;

    private final JobMatchingService jobMatchingService;
    private final LearningRoadmapRepository roadmapRepository;
    private final GroqService groqService;
    private final LearningRoadmapPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public LearningRoadmapServiceImpl(JobMatchingService jobMatchingService,
                                      LearningRoadmapRepository roadmapRepository,
                                      GroqService groqService,
                                      LearningRoadmapPromptBuilder promptBuilder,
                                      ObjectMapper objectMapper) {
        this.jobMatchingService = jobMatchingService;
        this.roadmapRepository = roadmapRepository;
        this.groqService = groqService;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public LearningRoadmap generateForMatch(UUID jobMatchId, Integer hoursPerWeek, AuthenticatedUser authenticatedUser) {
        // Ownership is enforced by the owning service (match -> resume -> user).
        JobMatch match = jobMatchingService.getByIdForUser(jobMatchId, authenticatedUser);
        int hours = resolveHours(hoursPerWeek);

        JobMatchAiResponse gaps = readMatchAnalysis(match);
        Job job = match.getJob();

        String userPrompt = promptBuilder.buildUserPrompt(
                job.getTitle(),
                job.getCompany(),
                job.getDescription(),
                match.getMatchScore(),
                gaps.matchingSkills(),
                gaps.missingSkills(),
                gaps.missingRequirements(),
                gaps.keywordGaps(),
                hours);

        RoadmapContent content = requestValidRoadmap(userPrompt).toContent(hours);

        LearningRoadmap roadmap = roadmapRepository.findByJobMatchId(jobMatchId).orElseGet(LearningRoadmap::new);
        roadmap.setJobMatch(match);
        roadmap.setTargetRole(truncate(job.getTitle()));
        roadmap.setRoadmapJson(writeJson(content));
        roadmap.setTotalTasks(content.totalTasks());
        roadmap.setCompletedTaskIds("[]"); // new plan, new task ids -> progress starts over

        LearningRoadmap saved = roadmapRepository.save(roadmap);
        return loadDetails(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningRoadmap getForMatch(UUID jobMatchId, AuthenticatedUser authenticatedUser) {
        jobMatchingService.getByIdForUser(jobMatchId, authenticatedUser);
        LearningRoadmap roadmap = roadmapRepository.findByJobMatchId(jobMatchId)
                .orElseThrow(() -> new ResourceNotFoundException("No learning roadmap has been generated for this match yet"));
        return loadDetails(roadmap);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningRoadmap getByIdForUser(UUID roadmapId, AuthenticatedUser authenticatedUser) {
        LearningRoadmap roadmap = roadmapRepository.findWithDetailsById(roadmapId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning roadmap not found"));
        requireOwner(roadmap, authenticatedUser);
        return loadDetails(roadmap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningRoadmap> listForUser(AuthenticatedUser authenticatedUser) {
        return roadmapRepository.findByJobMatchResumeUserClerkUserIdOrderByUpdatedAtDesc(authenticatedUser.clerkUserId())
                .stream()
                .map(this::loadDetails)
                .toList();
    }

    @Override
    @Transactional
    public LearningRoadmap setTaskCompleted(UUID roadmapId, String taskId, boolean completed,
                                            AuthenticatedUser authenticatedUser) {
        LearningRoadmap roadmap = roadmapRepository.findByIdForUpdate(roadmapId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning roadmap not found"));
        requireOwner(roadmap, authenticatedUser);

        RoadmapContent content = readContent(roadmap);
        if (taskId == null || !content.hasTask(taskId)) {
            throw new ValidationException("Task '" + taskId + "' is not part of this roadmap");
        }

        Set<String> done = readCompletedIds(roadmap);
        boolean changed = completed ? done.add(taskId) : done.remove(taskId);
        if (changed) {
            roadmap.setCompletedTaskIds(writeJson(done));
            roadmap = roadmapRepository.save(roadmap);
        }
        return loadDetails(roadmap);
    }

    @Override
    @Transactional
    public void deleteForUser(UUID roadmapId, AuthenticatedUser authenticatedUser) {
        LearningRoadmap roadmap = roadmapRepository.findWithDetailsById(roadmapId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning roadmap not found"));
        requireOwner(roadmap, authenticatedUser);
        roadmapRepository.delete(roadmap);
    }

    // --- helpers ---

    private LearningRoadmapAiResponse requestValidRoadmap(String userPrompt) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        InvalidAiResponseException lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String prompt = attempt == 1 ? userPrompt : promptBuilder.buildRetryUserPrompt(userPrompt);
            String raw = groqService.complete(systemPrompt, prompt);

            try {
                LearningRoadmapAiResponse parsed = objectMapper.readValue(raw, LearningRoadmapAiResponse.class);
                parsed.validate();
                return parsed;
            } catch (Exception ex) {
                log.warn("Groq learning roadmap response failed validation (attempt {}/{}): {}",
                        attempt, MAX_ATTEMPTS, ex.getMessage());
                lastError = new InvalidAiResponseException("AI response did not match the required schema", ex);
            }
        }

        throw lastError;
    }

    private void requireOwner(LearningRoadmap roadmap, AuthenticatedUser authenticatedUser) {
        String ownerClerkId = roadmap.getJobMatch().getResume().getUser().getClerkUserId();
        if (!ownerClerkId.equals(authenticatedUser.clerkUserId())) {
            throw new UnauthorizedResourceException("You do not have access to this learning roadmap");
        }
    }

    /**
     * open-in-view is off: initialize everything the controller's mapper reads
     * while the transaction is still open.
     */
    private LearningRoadmap loadDetails(LearningRoadmap roadmap) {
        JobMatch match = roadmap.getJobMatch();
        Hibernate.initialize(match);
        Hibernate.initialize(match.getJob());
        Hibernate.initialize(match.getResume());
        return roadmap;
    }

    private int resolveHours(Integer hoursPerWeek) {
        if (hoursPerWeek == null) {
            return DEFAULT_HOURS_PER_WEEK;
        }
        if (hoursPerWeek < MIN_HOURS_PER_WEEK || hoursPerWeek > MAX_HOURS_PER_WEEK) {
            throw new ValidationException("hoursPerWeek must be between " + MIN_HOURS_PER_WEEK + " and " + MAX_HOURS_PER_WEEK);
        }
        return hoursPerWeek;
    }

    private JobMatchAiResponse readMatchAnalysis(JobMatch match) {
        try {
            return objectMapper.readValue(match.getAnalysisJson(), JobMatchAiResponse.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Stored match JSON is corrupt for match " + match.getId(), ex);
        }
    }

    RoadmapContent readContent(LearningRoadmap roadmap) {
        try {
            return objectMapper.readValue(roadmap.getRoadmapJson(), RoadmapContent.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Stored roadmap JSON is corrupt for roadmap " + roadmap.getId(), ex);
        }
    }

    private Set<String> readCompletedIds(LearningRoadmap roadmap) {
        try {
            return new LinkedHashSet<>(objectMapper.readValue(roadmap.getCompletedTaskIds(), new TypeReference<List<String>>() { }));
        } catch (Exception ex) {
            throw new IllegalStateException("Stored progress JSON is corrupt for roadmap " + roadmap.getId(), ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new InvalidAiResponseException("Failed to serialize learning roadmap", ex);
        }
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() > MAX_ROLE_LENGTH ? trimmed.substring(0, MAX_ROLE_LENGTH) : trimmed;
    }
}
