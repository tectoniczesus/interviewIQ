package com.interviewiq.controller;

import com.interviewiq.dto.ApiResponse;
import com.interviewiq.dto.JobMatchCreateRequest;
import com.interviewiq.dto.JobMatchResponse;
import com.interviewiq.mapper.JobMatchMapper;
import com.interviewiq.security.CurrentUserProvider;
import com.interviewiq.service.JobMatchingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/job-matches")
public class JobMatchController {

    private final JobMatchingService jobMatchingService;
    private final JobMatchMapper jobMatchMapper;
    private final CurrentUserProvider currentUserProvider;

    public JobMatchController(JobMatchingService jobMatchingService,
                              JobMatchMapper jobMatchMapper,
                              CurrentUserProvider currentUserProvider) {
        this.jobMatchingService = jobMatchingService;
        this.jobMatchMapper = jobMatchMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobMatchResponse>> create(@Valid @RequestBody JobMatchCreateRequest request) {
        var match = jobMatchingService.createMatch(
                request.resumeId(), request.jobId(), currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(jobMatchMapper.toResponse(match)));
    }

    @GetMapping
    public ApiResponse<List<JobMatchResponse>> list() {
        var matches = jobMatchingService.listForUser(currentUserProvider.getCurrentUser())
                .stream()
                .map(jobMatchMapper::toResponse)
                .toList();
        return ApiResponse.ok(matches);
    }

    @GetMapping("/{id}")
    public ApiResponse<JobMatchResponse> get(@PathVariable UUID id) {
        var match = jobMatchingService.getByIdForUser(id, currentUserProvider.getCurrentUser());
        return ApiResponse.ok(jobMatchMapper.toResponse(match));
    }
}