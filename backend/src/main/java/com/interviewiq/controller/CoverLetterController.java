package com.interviewiq.controller;

import com.interviewiq.dto.ApiResponse;
import com.interviewiq.dto.CoverLetterCreateRequest;
import com.interviewiq.dto.CoverLetterResponse;
import com.interviewiq.security.CurrentUserProvider;
import com.interviewiq.service.CoverLetterService;
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
@RequestMapping("/api/cover-letters")
public class CoverLetterController {

    private final CoverLetterService coverLetterService;
    private final CurrentUserProvider currentUserProvider;

    public CoverLetterController(CoverLetterService coverLetterService, CurrentUserProvider currentUserProvider) {
        this.coverLetterService = coverLetterService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CoverLetterResponse>> create(@Valid @RequestBody CoverLetterCreateRequest request) {
        var coverLetter = coverLetterService.create(
                request.resumeId(), request.jobId(), request.candidateName(), currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(CoverLetterResponse.from(coverLetter)));
    }

    @GetMapping
    public ApiResponse<List<CoverLetterResponse>> list() {
        var coverLetters = coverLetterService.listForUser(currentUserProvider.getCurrentUser())
                .stream()
                .map(CoverLetterResponse::from)
                .toList();
        return ApiResponse.ok(coverLetters);
    }

    @GetMapping("/{id}")
    public ApiResponse<CoverLetterResponse> get(@PathVariable UUID id) {
        var coverLetter = coverLetterService.getByIdForUser(id, currentUserProvider.getCurrentUser());
        return ApiResponse.ok(CoverLetterResponse.from(coverLetter));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        coverLetterService.deleteForUser(id, currentUserProvider.getCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/regenerate")
    public ApiResponse<CoverLetterResponse> regenerate(@PathVariable UUID id) {
        var coverLetter = coverLetterService.regenerate(id, currentUserProvider.getCurrentUser());
        return ApiResponse.ok(CoverLetterResponse.from(coverLetter));
    }
}