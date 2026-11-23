package com.interviewiq.entity;

import com.interviewiq.entity.enums.ApplicationStatus;
import com.interviewiq.entity.enums.JobSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_user_id", columnList = "user_id")
})
public class Job extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title")
    private String title;

    @Column(name = "company")
    private String company;

    @Column(name = "job_url")
    private String jobUrl;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private JobSource source;

    // --- Application tracker ---

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false)
    private ApplicationStatus applicationStatus = ApplicationStatus.SAVED;

    @Column(name = "applied_on")
    private LocalDate appliedOn;

    @Column(name = "follow_up_on")
    private LocalDate followUpOn;

    @Column(name = "application_notes", columnDefinition = "TEXT")
    private String applicationNotes;

    /** When applicationStatus last changed; drives "days in stage" and stale nudges. */
    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    @PrePersist
    void initStatusChangedAt() {
        if (statusChangedAt == null) {
            statusChangedAt = Instant.now();
        }
    }

    public ApplicationStatus getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(ApplicationStatus applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public LocalDate getAppliedOn() {
        return appliedOn;
    }

    public void setAppliedOn(LocalDate appliedOn) {
        this.appliedOn = appliedOn;
    }

    public LocalDate getFollowUpOn() {
        return followUpOn;
    }

    public void setFollowUpOn(LocalDate followUpOn) {
        this.followUpOn = followUpOn;
    }

    public String getApplicationNotes() {
        return applicationNotes;
    }

    public void setApplicationNotes(String applicationNotes) {
        this.applicationNotes = applicationNotes;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(Instant statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JobSource getSource() {
        return source;
    }

    public void setSource(JobSource source) {
        this.source = source;
    }
}
