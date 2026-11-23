package com.interviewiq.service.impl;

import com.interviewiq.RoadmapTestData;
import com.interviewiq.dto.LearningRoadmapResponse;
import com.interviewiq.entity.Job;
import com.interviewiq.entity.JobMatch;
import com.interviewiq.entity.LearningRoadmap;
import com.interviewiq.entity.Resume;
import com.interviewiq.entity.User;
import com.interviewiq.entity.enums.JobSource;
import com.interviewiq.entity.enums.ProcessingStatus;
import com.interviewiq.exception.UnauthorizedResourceException;
import com.interviewiq.mapper.LearningRoadmapMapper;
import com.interviewiq.repository.JobMatchRepository;
import com.interviewiq.repository.JobRepository;
import com.interviewiq.repository.LearningRoadmapRepository;
import com.interviewiq.repository.ResumeRepository;
import com.interviewiq.repository.UserRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.GroqService;
import com.interviewiq.service.JobService;
import com.interviewiq.service.LearningRoadmapService;
import com.interviewiq.service.ResumeService;
import com.interviewiq.service.SupabaseStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Opt-in end-to-end test against the real Supabase Postgres (Groq + Storage mocked).
 * Exercises what unit tests can't: entity graphs with open-in-view disabled (mapping
 * happens OUTSIDE the transaction, exactly like the controllers), the unique
 * job_match_id constraint, the row lock on task toggles, and the real FK delete chain.
 * Creates throwaway rows under a random "it_roadmap_" Clerk id and removes them.
 *
 *   RUN_DB_IT=true mvn test -Dtest=LearningRoadmapDbIntegrationTest
 */
@EnabledIfEnvironmentVariable(named = "RUN_DB_IT", matches = "true")
@SpringBootTest // default MOCK web env: SecurityConfig needs Spring MVC, but no port is opened
class LearningRoadmapDbIntegrationTest {

    @MockBean
    private GroqService groqService;
    @MockBean
    private SupabaseStorageService storageService;

    @Autowired private LearningRoadmapService roadmapService;
    @Autowired private LearningRoadmapMapper mapper;
    @Autowired private JobService jobService;
    @Autowired private ResumeService resumeService;
    @Autowired private UserRepository userRepository;
    @Autowired private ResumeRepository resumeRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobMatchRepository jobMatchRepository;
    @Autowired private LearningRoadmapRepository roadmapRepository;

    private AuthenticatedUser owner;
    private User user;
    private Resume resume;
    private Job job;
    private JobMatch match;

    @BeforeEach
    void seed() {
        String clerkId = "it_roadmap_" + UUID.randomUUID();
        owner = new AuthenticatedUser(clerkId, clerkId + "@example.invalid");

        user = new User();
        user.setClerkUserId(clerkId);
        user.setEmail(owner.email());
        user = userRepository.save(user);

        resume = new Resume();
        resume.setUser(user);
        resume.setFileName("it-resume.pdf");
        resume.setStoragePath("it/does-not-exist.pdf");
        resume.setExtractedText("Java, SQL, REST APIs");
        resume.setProcessingStatus(ProcessingStatus.COMPLETED);
        resume = resumeRepository.save(resume);

        job = new Job();
        job.setUser(user);
        job.setTitle("Backend Developer");
        job.setCompany("Acme");
        job.setDescription("Java, Docker, Kubernetes");
        job.setSource(JobSource.PASTED);
        job = jobRepository.save(job);

        match = new JobMatch();
        match.setResume(resume);
        match.setJob(job);
        match.setMatchScore(62);
        match.setAnalysisJson(RoadmapTestData.MATCH_ANALYSIS_JSON);
        match = jobMatchRepository.save(match);

        when(groqService.complete(anyString(), anyString())).thenReturn(RoadmapTestData.VALID_ROADMAP_JSON);
    }

    @AfterEach
    void cleanUp() {
        // Deepest-first, tolerant of whatever the test already deleted.
        roadmapRepository.deleteAll(roadmapRepository.findByJobMatchResumeId(resume.getId()));
        jobMatchRepository.deleteAll(jobMatchRepository.findByResumeIdOrderByCreatedAtDesc(resume.getId()));
        jobRepository.findById(job.getId()).ifPresent(jobRepository::delete);
        resumeRepository.findById(resume.getId()).ifPresent(resumeRepository::delete);
        userRepository.findById(user.getId()).ifPresent(userRepository::delete);
    }

    @Test
    void fullRoadmapLifecycleAgainstRealPostgres() throws Exception {
        // generate -> mapped outside any transaction (open-in-view is off)
        LearningRoadmap created = roadmapService.generateForMatch(match.getId(), 10, owner);
        LearningRoadmapResponse response = mapper.toResponse(created);
        assertThat(response.jobTitle()).isEqualTo("Backend Developer");
        assertThat(response.resumeFileName()).isEqualTo("it-resume.pdf");
        assertThat(response.hoursPerWeek()).isEqualTo(10);
        assertThat(response.totalTasks()).isEqualTo(3);
        assertThat(response.progressPercent()).isZero();

        // progress persists
        mapper.toResponse(roadmapService.setTaskCompleted(created.getId(), "w1-t1", true, owner));
        LearningRoadmapResponse reloaded = mapper.toResponse(roadmapService.getForMatch(match.getId(), owner));
        assertThat(reloaded.completedTasks()).isEqualTo(1);
        assertThat(reloaded.weeks().get(0).tasks().get(0).completed()).isTrue();

        // list + summary mapping outside a transaction
        var summaries = roadmapService.listForUser(owner).stream().map(mapper::toSummary).toList();
        assertThat(summaries).singleElement().satisfies(s -> {
            assertThat(s.jobMatchId()).isEqualTo(match.getId());
            assertThat(s.progressPercent()).isEqualTo(33);
        });

        // another user can't read or toggle it
        var stranger = new AuthenticatedUser("it_roadmap_stranger_" + UUID.randomUUID(), "x@example.invalid");
        assertThatThrownBy(() -> roadmapService.getByIdForUser(created.getId(), stranger))
                .isInstanceOf(UnauthorizedResourceException.class);
        assertThatThrownBy(() -> roadmapService.setTaskCompleted(created.getId(), "w1-t2", true, stranger))
                .isInstanceOf(UnauthorizedResourceException.class);

        // regenerate replaces the same row (unique job_match_id) and resets progress
        LearningRoadmap regenerated = roadmapService.generateForMatch(match.getId(), 4, owner);
        assertThat(regenerated.getId()).isEqualTo(created.getId());
        assertThat(roadmapRepository.findByJobMatchResumeId(resume.getId())).hasSize(1);
        LearningRoadmapResponse afterRegen = mapper.toResponse(roadmapService.getByIdForUser(created.getId(), owner));
        assertThat(afterRegen.completedTasks()).isZero();
        assertThat(afterRegen.hoursPerWeek()).isEqualTo(4);
    }

    @Test
    void concurrentTogglesOfDifferentTasksAreBothKept() throws Exception {
        LearningRoadmap created = roadmapService.generateForMatch(match.getId(), 8, owner);
        UUID id = created.getId();

        CountDownLatch start = new CountDownLatch(1);
        CompletableFuture<Void> a = CompletableFuture.runAsync(() -> {
            await(start);
            roadmapService.setTaskCompleted(id, "w1-t1", true, owner);
        });
        CompletableFuture<Void> b = CompletableFuture.runAsync(() -> {
            await(start);
            roadmapService.setTaskCompleted(id, "w2-t1", true, owner);
        });
        start.countDown();
        CompletableFuture.allOf(a, b).get(30, TimeUnit.SECONDS);

        LearningRoadmapResponse result = mapper.toResponse(roadmapService.getByIdForUser(id, owner));
        assertThat(result.completedTasks()).as("row lock prevents a lost update").isEqualTo(2);
    }

    @Test
    void deletingJobOrResumeCascadesThroughRoadmapsWithoutFkErrors() {
        roadmapService.generateForMatch(match.getId(), 8, owner);

        jobService.deleteForUser(job.getId(), owner);

        assertThat(roadmapRepository.findByJobMatchResumeId(resume.getId())).isEmpty();
        assertThat(jobMatchRepository.findById(match.getId())).isEmpty();
        assertThat(jobRepository.findById(job.getId())).isEmpty();

        // and the resume chain, with a fresh match + roadmap
        Job job2 = new Job();
        job2.setUser(user);
        job2.setDescription("Another job");
        job2.setSource(JobSource.PASTED);
        job = jobRepository.save(job2);
        JobMatch match2 = new JobMatch();
        match2.setResume(resume);
        match2.setJob(job);
        match2.setMatchScore(40);
        match2.setAnalysisJson(RoadmapTestData.MATCH_ANALYSIS_JSON);
        match = jobMatchRepository.save(match2);
        roadmapService.generateForMatch(match.getId(), 8, owner);

        resumeService.deleteForUser(resume.getId(), owner);

        assertThat(roadmapRepository.findByJobMatchResumeId(resume.getId())).isEmpty();
        assertThat(resumeRepository.findById(resume.getId())).isEmpty();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
