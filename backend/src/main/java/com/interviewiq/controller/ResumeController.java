package com.interviewiq.controller;

import com.interviewiq.dto.ApiResponse;
import com.interviewiq.dto.ResumeAnalysisResponse;
import com.interviewiq.dto.ResumeDetailResponse;
import com.interviewiq.dto.ResumeSummaryResponse;
import com.interviewiq.mapper.ResumeAnalysisMapper;
import com.interviewiq.security.CurrentUserProvider;
import com.interviewiq.service.ResumeAnalysisService;
import com.interviewiq.service.ResumeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeAnalysisService resumeAnalysisService;
    private final ResumeAnalysisMapper resumeAnalysisMapper;
    private final CurrentUserProvider currentUserProvider;

    public ResumeController(ResumeService resumeService,
                            ResumeAnalysisService resumeAnalysisService,
                            ResumeAnalysisMapper resumeAnalysisMapper,
                            CurrentUserProvider currentUserProvider) {
        this.resumeService = resumeService;
        this.resumeAnalysisService = resumeAnalysisService;
        this.resumeAnalysisMapper = resumeAnalysisMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResumeDetailResponse>> upload(@RequestParam("file") MultipartFile file) {
        var resume = resumeService.upload(file, currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ResumeDetailResponse.from(resume)));
    }

    @GetMapping
    public ApiResponse<List<ResumeSummaryResponse>> list() {
        var resumes = resumeService.listForUser(currentUserProvider.getCurrentUser())
                .stream()
                .map(ResumeSummaryResponse::from)
                .toList();
        return ApiResponse.ok(resumes);
    }

    @GetMapping("/{id}")
    public ApiResponse<ResumeDetailResponse> get(@PathVariable UUID id) {
        var resume = resumeService.getByIdForUser(id, currentUserProvider.getCurrentUser());
        return ApiResponse.ok(ResumeDetailResponse.from(resume));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        resumeService.deleteForUser(id, currentUserProvider.getCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<ApiResponse<ResumeAnalysisResponse>> analyze(@PathVariable UUID id) {
        var analysis = resumeAnalysisService.analyze(id, currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(resumeAnalysisMapper.toResponse(analysis)));
    }

    @GetMapping("/{id}/analysis")
    public ApiResponse<ResumeAnalysisResponse> latestAnalysis(@PathVariable UUID id) {
        var analysis = resumeAnalysisService.getLatestForUser(id, currentUserProvider.getCurrentUser());
        return ApiResponse.ok(resumeAnalysisMapper.toResponse(analysis));
    }
}