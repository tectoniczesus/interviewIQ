package com.interviewiq.service.impl;

import com.interviewiq.dto.DashboardJobMatchItem;
import com.interviewiq.dto.DashboardResponse;
import com.interviewiq.dto.DashboardResumeItem;
import com.interviewiq.entity.Resume;
import com.interviewiq.repository.CoverLetterRepository;
import com.interviewiq.repository.InterviewSessionRepository;
import com.interviewiq.repository.JobMatchRepository;
import com.interviewiq.repository.ResumeAnalysisRepository;
import com.interviewiq.repository.ResumeRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final int RECENT_LIMIT = 5;

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final JobMatchRepository jobMatchRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final InterviewSessionRepository interviewSessionRepository;

    public DashboardServiceImpl(ResumeRepository resumeRepository,
                                ResumeAnalysisRepository resumeAnalysisRepository,
                                JobMatchRepository jobMatchRepository,
                                CoverLetterRepository coverLetterRepository,
                                InterviewSessionRepository interviewSessionRepository) {
        this.resumeRepository = resumeRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.coverLetterRepository = coverLetterRepository;
        this.interviewSessionRepository = interviewSessionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getSummary(AuthenticatedUser authenticatedUser) {
        String clerkUserId = authenticatedUser.clerkUserId();

        Integer resumeScore = resumeAnalysisRepository
                .findFirstByResumeUserClerkUserIdOrderByCreatedAtDesc(clerkUserId)
                .map(a -> a.getOverallScore())
                .orElse(null);

        long resumeCount = resumeRepository.countByUserClerkUserId(clerkUserId);
        long jobMatchCount = jobMatchRepository.countByResumeUserClerkUserId(clerkUserId);
        long coverLetterCount = coverLetterRepository.countByUserClerkUserId(clerkUserId);
        long interviewSessionCount = interviewSessionRepository.countByUserClerkUserId(clerkUserId);

        // All entity field access (including lazy associations) happens right
        // here, inside this @Transactional method, so the DTOs below are fully
        // built before the session closes - see ARCHITECTURE.md note on the
        // InterviewQuestion.keyPoints bug for why this matters.
        List<DashboardResumeItem> recentResumes = resumeRepository
                .findByUserClerkUserIdOrderByCreatedAtDesc(clerkUserId)
                .stream()
                .limit(RECENT_LIMIT)
                .map(this::toResumeItem)
                .toList();

        List<DashboardJobMatchItem> recentJobMatches = jobMatchRepository
                .findByResumeUserClerkUserIdOrderByCreatedAtDesc(clerkUserId)
                .stream()
                .limit(RECENT_LIMIT)
                .map(match -> new DashboardJobMatchItem(
                        match.getId(),
                        match.getJob().getId(),
                        match.getJob().getTitle(),
                        match.getJob().getCompany(),
                        match.getMatchScore(),
                        match.getCreatedAt()
                ))
                .toList();

        return new DashboardResponse(
                resumeScore, resumeCount, jobMatchCount, coverLetterCount, interviewSessionCount,
                recentResumes, recentJobMatches
        );
    }

    private DashboardResumeItem toResumeItem(Resume resume) {
        Integer atsScore = resumeAnalysisRepository
                .findFirstByResumeIdOrderByCreatedAtDesc(resume.getId())
                .map(a -> a.getAtsScore())
                .orElse(null);

        return new DashboardResumeItem(resume.getId(), resume.getFileName(), atsScore, resume.getCreatedAt());
    }
}