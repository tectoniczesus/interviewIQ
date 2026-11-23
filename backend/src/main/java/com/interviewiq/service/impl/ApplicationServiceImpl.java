package com.interviewiq.service.impl;

import com.interviewiq.dto.ApplicationEventResponse;
import com.interviewiq.dto.ApplicationResponse;
import com.interviewiq.dto.ApplicationUpdateRequest;
import com.interviewiq.entity.ApplicationEvent;
import com.interviewiq.entity.Job;
import com.interviewiq.entity.JobMatch;
import com.interviewiq.entity.enums.ApplicationStatus;
import com.interviewiq.repository.ApplicationEventRepository;
import com.interviewiq.repository.JobMatchRepository;
import com.interviewiq.repository.JobRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.ApplicationService;
import com.interviewiq.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    static final int DEFAULT_FOLLOW_UP_DAYS = 7;

    private final JobService jobService;
    private final JobRepository jobRepository;
    private final JobMatchRepository jobMatchRepository;
    private final ApplicationEventRepository eventRepository;
    private final Clock clock;

    @Autowired
    public ApplicationServiceImpl(JobService jobService,
                                  JobRepository jobRepository,
                                  JobMatchRepository jobMatchRepository,
                                  ApplicationEventRepository eventRepository) {
        this(jobService, jobRepository, jobMatchRepository, eventRepository, Clock.systemUTC());
    }

    ApplicationServiceImpl(JobService jobService,
                           JobRepository jobRepository,
                           JobMatchRepository jobMatchRepository,
                           ApplicationEventRepository eventRepository,
                           Clock clock) {
        this.jobService = jobService;
        this.jobRepository = jobRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> listForUser(AuthenticatedUser authenticatedUser) {
        String clerkUserId = authenticatedUser.clerkUserId();

        // One query for all of the user's matches (newest first); keep the first
        // score seen per job. getJob().getId() reads the FK without loading the job.
        Map<UUID, Integer> latestScoreByJob = new HashMap<>();
        for (JobMatch match : jobMatchRepository.findByResumeUserClerkUserIdOrderByCreatedAtDesc(clerkUserId)) {
            latestScoreByJob.putIfAbsent(match.getJob().getId(), match.getMatchScore());
        }

        return jobRepository.findByUserClerkUserIdOrderByCreatedAtDesc(clerkUserId)
                .stream()
                .map(job -> ApplicationResponse.from(job, latestScoreByJob.get(job.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getForUser(UUID jobId, AuthenticatedUser authenticatedUser) {
        Job job = jobService.getByIdForUser(jobId, authenticatedUser);
        return ApplicationResponse.from(job, latestScore(jobId));
    }

    @Override
    @Transactional
    public ApplicationResponse update(UUID jobId, ApplicationUpdateRequest request, AuthenticatedUser authenticatedUser) {
        Job job = jobService.getByIdForUser(jobId, authenticatedUser);
        ApplicationStatus previous = job.getApplicationStatus();
        ApplicationStatus next = request.status();

        LocalDate appliedOn = request.appliedOn();
        LocalDate followUpOn = request.followUpOn();

        if (next != previous) {
            if (next == ApplicationStatus.APPLIED) {
                if (appliedOn == null) {
                    appliedOn = LocalDate.now(clock);
                }
                if (followUpOn == null) {
                    followUpOn = appliedOn.plusDays(DEFAULT_FOLLOW_UP_DAYS);
                }
            }
            job.setApplicationStatus(next);
            job.setStatusChangedAt(Instant.now(clock));

            ApplicationEvent event = new ApplicationEvent();
            event.setJob(job);
            event.setFromStatus(previous);
            event.setToStatus(next);
            eventRepository.save(event);
        }

        job.setAppliedOn(appliedOn);
        job.setFollowUpOn(followUpOn);
        job.setApplicationNotes(blankToNull(request.notes()));

        Job saved = jobRepository.save(job);
        return ApplicationResponse.from(saved, latestScore(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationEventResponse> listEvents(UUID jobId, AuthenticatedUser authenticatedUser) {
        jobService.getByIdForUser(jobId, authenticatedUser);
        return eventRepository.findByJobIdOrderByCreatedAtAsc(jobId)
                .stream()
                .map(ApplicationEventResponse::from)
                .toList();
    }

    private Integer latestScore(UUID jobId) {
        return jobMatchRepository.findByJobIdOrderByCreatedAtDesc(jobId)
                .stream()
                .findFirst()
                .map(JobMatch::getMatchScore)
                .orElse(null);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
