package com.interviewiq.service.impl;

import com.interviewiq.dto.ApplicationResponse;
import com.interviewiq.dto.ApplicationUpdateRequest;
import com.interviewiq.entity.ApplicationEvent;
import com.interviewiq.entity.Job;
import com.interviewiq.entity.JobMatch;
import com.interviewiq.entity.enums.ApplicationStatus;
import com.interviewiq.exception.UnauthorizedResourceException;
import com.interviewiq.repository.ApplicationEventRepository;
import com.interviewiq.repository.JobMatchRepository;
import com.interviewiq.repository.JobRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    private static final AuthenticatedUser OWNER = new AuthenticatedUser("user_owner", "o@example.com");
    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    @Mock private JobService jobService;
    @Mock private JobRepository jobRepository;
    @Mock private JobMatchRepository jobMatchRepository;
    @Mock private ApplicationEventRepository eventRepository;

    private ApplicationServiceImpl service;
    private Job job;

    @BeforeEach
    void setUp() {
        service = new ApplicationServiceImpl(jobService, jobRepository, jobMatchRepository, eventRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
        job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("Backend Engineer");
        job.setStatusChangedAt(NOW.minusSeconds(86_400 * 10));
    }

    private void stubOwnedJob() {
        when(jobService.getByIdForUser(job.getId(), OWNER)).thenReturn(job);
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobMatchRepository.findByJobIdOrderByCreatedAtDesc(job.getId())).thenReturn(List.of());
    }

    @Test
    void movingToAppliedFillsAppliedDateAndDefaultFollowUpAndRecordsEvent() {
        stubOwnedJob();

        ApplicationResponse result = service.update(job.getId(),
                new ApplicationUpdateRequest(ApplicationStatus.APPLIED, null, null, "  referral from Sam  "), OWNER);

        assertThat(result.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(result.appliedOn()).isEqualTo(TODAY);
        assertThat(result.followUpOn()).isEqualTo(TODAY.plusDays(7));
        assertThat(result.notes()).isEqualTo("referral from Sam");
        assertThat(result.statusChangedAt()).isEqualTo(NOW);

        ArgumentCaptor<ApplicationEvent> event = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getFromStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(event.getValue().getToStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void clientSuppliedDatesWinOverDefaults() {
        stubOwnedJob();
        LocalDate applied = LocalDate.of(2026, 11, 10);
        LocalDate followUp = LocalDate.of(2026, 11, 20);

        ApplicationResponse result = service.update(job.getId(),
                new ApplicationUpdateRequest(ApplicationStatus.APPLIED, applied, followUp, null), OWNER);

        assertThat(result.appliedOn()).isEqualTo(applied);
        assertThat(result.followUpOn()).isEqualTo(followUp);
    }

    @Test
    void editingWithoutStageChangeRecordsNoEventAndKeepsStageClock() {
        job.setApplicationStatus(ApplicationStatus.APPLIED);
        Instant changedAt = job.getStatusChangedAt();
        stubOwnedJob();

        // Clearing the follow-up date while staying in APPLIED must stick (no default re-applied).
        ApplicationResponse result = service.update(job.getId(),
                new ApplicationUpdateRequest(ApplicationStatus.APPLIED, TODAY, null, "   "), OWNER);

        assertThat(result.followUpOn()).isNull();
        assertThat(result.notes()).isNull();
        assertThat(result.statusChangedAt()).isEqualTo(changedAt);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void responseCarriesLatestMatchScore() {
        when(jobService.getByIdForUser(job.getId(), OWNER)).thenReturn(job);
        JobMatch match = new JobMatch();
        match.setMatchScore(74);
        when(jobMatchRepository.findByJobIdOrderByCreatedAtDesc(job.getId())).thenReturn(List.of(match));

        assertThat(service.getForUser(job.getId(), OWNER).latestMatchScore()).isEqualTo(74);
    }

    @Test
    void ownershipFailureStopsUpdate() {
        when(jobService.getByIdForUser(job.getId(), OWNER))
                .thenThrow(new UnauthorizedResourceException("You do not have access to this job"));

        assertThatThrownBy(() -> service.update(job.getId(),
                new ApplicationUpdateRequest(ApplicationStatus.OFFER, null, null, null), OWNER))
                .isInstanceOf(UnauthorizedResourceException.class);
        verify(jobRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }
}
