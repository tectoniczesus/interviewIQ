package com.interviewiq.service.impl;

import com.interviewiq.entity.Job;
import com.interviewiq.entity.Resume;
import com.interviewiq.entity.User;
import com.interviewiq.repository.ApplicationEventRepository;
import com.interviewiq.repository.CoverLetterRepository;
import com.interviewiq.repository.InterviewQuestionRepository;
import com.interviewiq.repository.InterviewSessionRepository;
import com.interviewiq.repository.JobMatchRepository;
import com.interviewiq.repository.JobRepository;
import com.interviewiq.repository.LearningRoadmapRepository;
import com.interviewiq.repository.ResumeAnalysisRepository;
import com.interviewiq.repository.ResumeRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.FirecrawlService;
import com.interviewiq.service.PdfExtractionService;
import com.interviewiq.service.SupabaseStorageService;
import com.interviewiq.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * Postgres has no cascades here: learning_roadmaps -> job_matches is a
 * required FK, so roadmaps must be deleted before job matches in both
 * the resume and job delete chains.
 *
 * The upload/URL validators are concrete classes that deleteForUser never
 * touches, so they're passed as null: Mockito's inline mocking of concrete
 * classes doesn't work on JDK 25 with the Byte Buddy version Boot 3.3 ships.
 */
@ExtendWith(MockitoExtension.class)
class DeleteChainOrderTest {

    private static final AuthenticatedUser OWNER = new AuthenticatedUser("user_owner", "o@example.com");

    @Mock private ResumeRepository resumeRepository;
    @Mock private JobRepository jobRepository;
    @Mock private SupabaseStorageService storageService;
    @Mock private PdfExtractionService pdfExtractionService;
    @Mock private FirecrawlService firecrawlService;
    @Mock private UserService userService;
    @Mock private ResumeAnalysisRepository resumeAnalysisRepository;
    @Mock private JobMatchRepository jobMatchRepository;
    @Mock private CoverLetterRepository coverLetterRepository;
    @Mock private InterviewSessionRepository interviewSessionRepository;
    @Mock private InterviewQuestionRepository interviewQuestionRepository;
    @Mock private LearningRoadmapRepository learningRoadmapRepository;
    @Mock private ApplicationEventRepository applicationEventRepository;

    private static User owner() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setClerkUserId(OWNER.clerkUserId());
        return user;
    }

    @Test
    void resumeDeleteRemovesRoadmapsBeforeJobMatches() {
        Resume resume = new Resume();
        resume.setId(UUID.randomUUID());
        resume.setUser(owner());
        resume.setStoragePath("path/resume.pdf");
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));
        when(learningRoadmapRepository.findByJobMatchResumeId(resume.getId())).thenReturn(List.of());
        when(jobMatchRepository.findByResumeIdOrderByCreatedAtDesc(resume.getId())).thenReturn(List.of());

        var service = new ResumeServiceImpl(resumeRepository, null, storageService, pdfExtractionService,
                userService, resumeAnalysisRepository, jobMatchRepository, coverLetterRepository,
                interviewSessionRepository, interviewQuestionRepository, learningRoadmapRepository);

        service.deleteForUser(resume.getId(), OWNER);

        InOrder order = inOrder(learningRoadmapRepository, jobMatchRepository, resumeRepository);
        order.verify(learningRoadmapRepository).deleteAll(anyList());
        order.verify(jobMatchRepository).deleteAll(anyList());
        order.verify(resumeRepository).delete(resume);
    }

    @Test
    void jobDeleteRemovesRoadmapsBeforeJobMatches() {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setUser(owner());
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(learningRoadmapRepository.findByJobMatchJobId(job.getId())).thenReturn(List.of());
        when(jobMatchRepository.findByJobIdOrderByCreatedAtDesc(job.getId())).thenReturn(List.of());

        var service = new JobServiceImpl(jobRepository, firecrawlService, null, userService,
                jobMatchRepository, coverLetterRepository, interviewSessionRepository, interviewQuestionRepository,
                learningRoadmapRepository, applicationEventRepository);

        service.deleteForUser(job.getId(), OWNER);

        InOrder order = inOrder(learningRoadmapRepository, jobMatchRepository, jobRepository);
        order.verify(learningRoadmapRepository).deleteAll(anyList());
        order.verify(jobMatchRepository).deleteAll(anyList());
        order.verify(jobRepository).delete(job);
    }

    @Test
    void jobDeleteRemovesApplicationEventsBeforeJob() {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setUser(owner());
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(applicationEventRepository.findByJobIdOrderByCreatedAtAsc(job.getId())).thenReturn(List.of());

        var service = new JobServiceImpl(jobRepository, firecrawlService, null, userService,
                jobMatchRepository, coverLetterRepository, interviewSessionRepository, interviewQuestionRepository,
                learningRoadmapRepository, applicationEventRepository);

        service.deleteForUser(job.getId(), OWNER);

        InOrder order = inOrder(applicationEventRepository, jobRepository);
        order.verify(applicationEventRepository).deleteAll(anyList());
        order.verify(jobRepository).delete(job);
    }
}
