package com.interviewiq.controller;

import com.interviewiq.dto.ApiResponse;
import com.interviewiq.dto.InterviewQuestionResponse;
import com.interviewiq.dto.InterviewSessionCreateRequest;
import com.interviewiq.dto.InterviewSessionResponse;
import com.interviewiq.entity.enums.QuestionCategory;
import com.interviewiq.security.CurrentUserProvider;
import com.interviewiq.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;
    private final CurrentUserProvider currentUserProvider;

    public InterviewController(InterviewService interviewService, CurrentUserProvider currentUserProvider) {
        this.interviewService = interviewService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InterviewSessionResponse>> create(
            @Valid @RequestBody InterviewSessionCreateRequest request) {
        var session = interviewService.create(
                request.resumeId(), request.jobId(), request.role(), currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(InterviewSessionResponse.from(session)));
    }

    @GetMapping
    public ApiResponse<List<InterviewSessionResponse>> list() {
        var sessions = interviewService.listForUser(currentUserProvider.getCurrentUser())
                .stream()
                .map(InterviewSessionResponse::from)
                .toList();
        return ApiResponse.ok(sessions);
    }

    @GetMapping("/{id}")
    public ApiResponse<InterviewSessionResponse> get(@PathVariable UUID id) {
        var session = interviewService.getByIdForUser(id, currentUserProvider.getCurrentUser());
        return ApiResponse.ok(InterviewSessionResponse.from(session));
    }

    @GetMapping("/{id}/questions")
    public ApiResponse<List<InterviewQuestionResponse>> questions(
            @PathVariable UUID id,
            @RequestParam(required = false) QuestionCategory category) {
        var questions = interviewService.getQuestionsForUser(id, category, currentUserProvider.getCurrentUser())
                .stream()
                .map(InterviewQuestionResponse::from)
                .toList();
        return ApiResponse.ok(questions);
    }

    @PostMapping("/{id}/regenerate")
    public ApiResponse<List<InterviewQuestionResponse>> regenerate(@PathVariable UUID id) {
        var questions = interviewService.regenerate(id, currentUserProvider.getCurrentUser())
                .stream()
                .map(InterviewQuestionResponse::from)
                .toList();
        return ApiResponse.ok(questions);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        interviewService.deleteForUser(id, currentUserProvider.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}