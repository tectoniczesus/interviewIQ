package com.interviewiq.controller;

import com.interviewiq.dto.ApiResponse;
import com.interviewiq.dto.ApplicationEventResponse;
import com.interviewiq.dto.ApplicationResponse;
import com.interviewiq.dto.ApplicationUpdateRequest;
import com.interviewiq.security.CurrentUserProvider;
import com.interviewiq.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Application tracker. Applications are keyed by job id (one per saved job). */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final CurrentUserProvider currentUserProvider;

    public ApplicationController(ApplicationService applicationService, CurrentUserProvider currentUserProvider) {
        this.applicationService = applicationService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<List<ApplicationResponse>> list() {
        return ApiResponse.ok(applicationService.listForUser(currentUserProvider.getCurrentUser()));
    }

    @GetMapping("/{jobId}")
    public ApiResponse<ApplicationResponse> get(@PathVariable UUID jobId) {
        return ApiResponse.ok(applicationService.getForUser(jobId, currentUserProvider.getCurrentUser()));
    }

    @PutMapping("/{jobId}")
    public ApiResponse<ApplicationResponse> update(@PathVariable UUID jobId,
                                                   @Valid @RequestBody ApplicationUpdateRequest request) {
        return ApiResponse.ok(applicationService.update(jobId, request, currentUserProvider.getCurrentUser()));
    }

    @GetMapping("/{jobId}/events")
    public ApiResponse<List<ApplicationEventResponse>> events(@PathVariable UUID jobId) {
        return ApiResponse.ok(applicationService.listEvents(jobId, currentUserProvider.getCurrentUser()));
    }
}
