package com.interviewiq.controller;

import com.interviewiq.dto.ApiResponse;
import com.interviewiq.dto.JobCreateRequest;
import com.interviewiq.dto.JobFromUrlRequest;
import com.interviewiq.dto.JobResponse;
import com.interviewiq.dto.JobSummaryResponse;
import com.interviewiq.security.CurrentUserProvider;
import com.interviewiq.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final CurrentUserProvider currentUserProvider;

    public JobController(JobService jobService, CurrentUserProvider currentUserProvider) {
        this.jobService = jobService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> create(@Valid @RequestBody JobCreateRequest request) {
        var job = jobService.createFromDescription(
                request.title(), request.company(), request.description(), currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(JobResponse.from(job)));
    }

    @PostMapping("/from-url")
    public ResponseEntity<ApiResponse<JobResponse>> createFromUrl(@Valid @RequestBody JobFromUrlRequest request) {
        var job = jobService.createFromUrl(
                request.title(), request.company(), request.jobUrl(), currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(JobResponse.from(job)));
    }

    @GetMapping
    public ApiResponse<List<JobSummaryResponse>> list() {
        var jobs = jobService.listForUser(currentUserProvider.getCurrentUser())
                .stream()
                .map(JobSummaryResponse::from)
                .toList();
        return ApiResponse.ok(jobs);
    }

    @GetMapping("/{id}")
    public ApiResponse<JobResponse> get(@PathVariable UUID id) {
        var job = jobService.getByIdForUser(id, currentUserProvider.getCurrentUser());
        return ApiResponse.ok(JobResponse.from(job));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        jobService.deleteForUser(id, currentUserProvider.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}