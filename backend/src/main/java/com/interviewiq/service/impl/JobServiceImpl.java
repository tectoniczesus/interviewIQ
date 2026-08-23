package com.interviewiq.service.impl;

import com.interviewiq.entity.Job;
import com.interviewiq.entity.User;
import com.interviewiq.entity.enums.JobSource;
import com.interviewiq.exception.ResourceNotFoundException;
import com.interviewiq.exception.UnauthorizedResourceException;
import com.interviewiq.exception.ValidationException;
import com.interviewiq.repository.CoverLetterRepository;
import com.interviewiq.repository.InterviewQuestionRepository;
import com.interviewiq.repository.InterviewSessionRepository;
import com.interviewiq.repository.JobMatchRepository;
import com.interviewiq.repository.JobRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.FirecrawlService;
import com.interviewiq.service.JobService;
import com.interviewiq.service.JobUrlValidator;
import com.interviewiq.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final FirecrawlService firecrawlService;
    private final JobUrlValidator jobUrlValidator;
    private final UserService userService;
    private final JobMatchRepository jobMatchRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;

    public JobServiceImpl(JobRepository jobRepository,
                          FirecrawlService firecrawlService,
                          JobUrlValidator jobUrlValidator,
                          UserService userService,
                          JobMatchRepository jobMatchRepository,
                          CoverLetterRepository coverLetterRepository,
                          InterviewSessionRepository interviewSessionRepository,
                          InterviewQuestionRepository interviewQuestionRepository) {
        this.jobRepository = jobRepository;
        this.firecrawlService = firecrawlService;
        this.jobUrlValidator = jobUrlValidator;
        this.userService = userService;
        this.jobMatchRepository = jobMatchRepository;
        this.coverLetterRepository = coverLetterRepository;
        this.interviewSessionRepository = interviewSessionRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
    }

    @Override
    @Transactional
    public Job createFromDescription(String title, String company, String description,
                                     AuthenticatedUser authenticatedUser) {
        if (description == null || description.isBlank()) {
            throw new ValidationException("Please provide a job description");
        }

        User user = userService.findOrCreate(authenticatedUser);

        Job job = new Job();
        job.setUser(user);
        job.setTitle(blankToNull(title));
        job.setCompany(blankToNull(company));
        job.setDescription(description.trim());
        job.setSource(JobSource.PASTED);

        return jobRepository.save(job);
    }

    @Override
    @Transactional
    public Job createFromUrl(String title, String company, String jobUrl, AuthenticatedUser authenticatedUser) {
        jobUrlValidator.validate(jobUrl);

        String extracted = firecrawlService.scrapeUrl(jobUrl.trim());

        User user = userService.findOrCreate(authenticatedUser);

        Job job = new Job();
        job.setUser(user);
        job.setTitle(blankToNull(title));
        job.setCompany(blankToNull(company));
        job.setJobUrl(jobUrl.trim());
        job.setDescription(extracted);
        job.setSource(JobSource.URL);

        return jobRepository.save(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> listForUser(AuthenticatedUser authenticatedUser) {
        return jobRepository.findByUserClerkUserIdOrderByCreatedAtDesc(authenticatedUser.clerkUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public Job getByIdForUser(UUID jobId, AuthenticatedUser authenticatedUser) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        assertOwnership(job, authenticatedUser);
        return job;
    }

    @Override
    @Transactional
    public void deleteForUser(UUID jobId, AuthenticatedUser authenticatedUser) {
        Job job = getByIdForUser(jobId, authenticatedUser);

        // Delete every record that has a required (non-nullable) foreign key to
        // this job, deepest dependents first, or Postgres rejects the final
        // delete with a constraint violation.
        interviewSessionRepository.findByJobId(jobId).forEach(session -> {
            interviewQuestionRepository.deleteAll(interviewQuestionRepository.findBySessionId(session.getId()));
        });
        interviewSessionRepository.deleteAll(interviewSessionRepository.findByJobId(jobId));
        coverLetterRepository.deleteAll(coverLetterRepository.findByJobId(jobId));
        jobMatchRepository.deleteAll(jobMatchRepository.findByJobIdOrderByCreatedAtDesc(jobId));

        jobRepository.delete(job);
    }

    private void assertOwnership(Job job, AuthenticatedUser authenticatedUser) {
        if (!job.getUser().getClerkUserId().equals(authenticatedUser.clerkUserId())) {
            throw new UnauthorizedResourceException("You do not have access to this job");
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}