package com.interviewiq.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.dto.LearningRoadmapResponse;
import com.interviewiq.dto.LearningRoadmapSummaryResponse;
import com.interviewiq.dto.RoadmapContent;
import com.interviewiq.entity.Job;
import com.interviewiq.entity.JobMatch;
import com.interviewiq.entity.LearningRoadmap;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class LearningRoadmapMapper {

    private final ObjectMapper objectMapper;

    public LearningRoadmapMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LearningRoadmapResponse toResponse(LearningRoadmap roadmap) {
        RoadmapContent content = readContent(roadmap);
        Set<String> done = readCompleted(roadmap);
        JobMatch match = roadmap.getJobMatch();
        Job job = match.getJob();

        List<LearningRoadmapResponse.WeekResponse> weeks = content.weeks().stream()
                .map(w -> new LearningRoadmapResponse.WeekResponse(
                        w.week(),
                        w.focus(),
                        w.skills(),
                        w.milestone(),
                        w.tasks().stream()
                                .map(t -> new LearningRoadmapResponse.TaskResponse(
                                        t.id(), t.title(), t.description(), t.type(),
                                        t.resourceName(), t.searchQuery(), done.contains(t.id())))
                                .toList()))
                .toList();

        int completed = countCompleted(content, done);
        return new LearningRoadmapResponse(
                roadmap.getId(),
                match.getId(),
                job.getId(),
                match.getResume().getId(),
                job.getTitle(),
                job.getCompany(),
                match.getResume().getFileName(),
                match.getMatchScore(),
                content.summary(),
                content.hoursPerWeek(),
                weeks,
                content.totalTasks(),
                completed,
                percent(completed, content.totalTasks()),
                roadmap.getCreatedAt(),
                roadmap.getUpdatedAt()
        );
    }

    public LearningRoadmapSummaryResponse toSummary(LearningRoadmap roadmap) {
        RoadmapContent content = readContent(roadmap);
        int completed = countCompleted(content, readCompleted(roadmap));
        JobMatch match = roadmap.getJobMatch();
        Job job = match.getJob();
        return new LearningRoadmapSummaryResponse(
                roadmap.getId(),
                match.getId(),
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                match.getResume().getFileName(),
                match.getMatchScore(),
                content.weeks().size(),
                content.totalTasks(),
                completed,
                percent(completed, content.totalTasks()),
                roadmap.getUpdatedAt()
        );
    }

    /** Only counts ids that still exist in the plan, so stale ids can never push progress past 100%. */
    private static int countCompleted(RoadmapContent content, Set<String> done) {
        return (int) content.weeks().stream()
                .flatMap(w -> w.tasks().stream())
                .filter(t -> done.contains(t.id()))
                .count();
    }

    private static int percent(int completed, int total) {
        return total == 0 ? 0 : Math.round(completed * 100f / total);
    }

    private RoadmapContent readContent(LearningRoadmap roadmap) {
        try {
            return objectMapper.readValue(roadmap.getRoadmapJson(), RoadmapContent.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Stored roadmap JSON is corrupt for roadmap " + roadmap.getId(), ex);
        }
    }

    private Set<String> readCompleted(LearningRoadmap roadmap) {
        try {
            return new HashSet<>(objectMapper.readValue(roadmap.getCompletedTaskIds(), new TypeReference<List<String>>() { }));
        } catch (Exception ex) {
            throw new IllegalStateException("Stored progress JSON is corrupt for roadmap " + roadmap.getId(), ex);
        }
    }
}
