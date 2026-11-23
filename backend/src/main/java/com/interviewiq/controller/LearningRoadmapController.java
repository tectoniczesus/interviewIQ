package com.interviewiq.controller;

import com.interviewiq.dto.ApiResponse;
import com.interviewiq.dto.LearningRoadmapResponse;
import com.interviewiq.dto.LearningRoadmapSummaryResponse;
import com.interviewiq.dto.RoadmapGenerateRequest;
import com.interviewiq.dto.RoadmapTaskUpdateRequest;
import com.interviewiq.mapper.LearningRoadmapMapper;
import com.interviewiq.security.CurrentUserProvider;
import com.interviewiq.service.LearningRoadmapService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LearningRoadmapController {

    private final LearningRoadmapService roadmapService;
    private final LearningRoadmapMapper roadmapMapper;
    private final CurrentUserProvider currentUserProvider;

    public LearningRoadmapController(LearningRoadmapService roadmapService,
                                     LearningRoadmapMapper roadmapMapper,
                                     CurrentUserProvider currentUserProvider) {
        this.roadmapService = roadmapService;
        this.roadmapMapper = roadmapMapper;
        this.currentUserProvider = currentUserProvider;
    }

    /** Generates a roadmap for the match, or regenerates it (replacing the plan and resetting progress). */
    @PostMapping("/job-matches/{matchId}/roadmap")
    public ResponseEntity<ApiResponse<LearningRoadmapResponse>> generate(
            @PathVariable UUID matchId,
            @Valid @RequestBody(required = false) RoadmapGenerateRequest request) {
        Integer hours = request == null ? null : request.hoursPerWeek();
        var roadmap = roadmapService.generateForMatch(matchId, hours, currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(roadmapMapper.toResponse(roadmap)));
    }

    @GetMapping("/job-matches/{matchId}/roadmap")
    public ApiResponse<LearningRoadmapResponse> getForMatch(@PathVariable UUID matchId) {
        var roadmap = roadmapService.getForMatch(matchId, currentUserProvider.getCurrentUser());
        return ApiResponse.ok(roadmapMapper.toResponse(roadmap));
    }

    @GetMapping("/roadmaps")
    public ApiResponse<List<LearningRoadmapSummaryResponse>> list() {
        var roadmaps = roadmapService.listForUser(currentUserProvider.getCurrentUser())
                .stream()
                .map(roadmapMapper::toSummary)
                .toList();
        return ApiResponse.ok(roadmaps);
    }

    @GetMapping("/roadmaps/{id}")
    public ApiResponse<LearningRoadmapResponse> get(@PathVariable UUID id) {
        var roadmap = roadmapService.getByIdForUser(id, currentUserProvider.getCurrentUser());
        return ApiResponse.ok(roadmapMapper.toResponse(roadmap));
    }

    @PatchMapping("/roadmaps/{id}/tasks/{taskId}")
    public ApiResponse<LearningRoadmapResponse> updateTask(
            @PathVariable UUID id,
            @PathVariable String taskId,
            @Valid @RequestBody RoadmapTaskUpdateRequest request) {
        var roadmap = roadmapService.setTaskCompleted(
                id, taskId, request.completed(), currentUserProvider.getCurrentUser());
        return ApiResponse.ok(roadmapMapper.toResponse(roadmap));
    }

    @DeleteMapping("/roadmaps/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roadmapService.deleteForUser(id, currentUserProvider.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
