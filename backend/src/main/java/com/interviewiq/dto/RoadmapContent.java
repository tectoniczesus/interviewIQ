package com.interviewiq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The validated roadmap as persisted in LearningRoadmap.roadmapJson.
 * Unlike the raw AI response, every task carries a stable server-assigned
 * id ("w{week}-t{n}") that progress tracking refers to.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RoadmapContent(
        String summary,
        int hoursPerWeek,
        List<Week> weeks
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Week(
            int week,
            String focus,
            List<String> skills,
            String milestone,
            List<Task> tasks
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Task(
            String id,
            String title,
            String description,
            String type,
            String resourceName,
            String searchQuery
    ) {
    }

    public int totalTasks() {
        return weeks.stream().mapToInt(w -> w.tasks().size()).sum();
    }

    public boolean hasTask(String taskId) {
        return weeks.stream().flatMap(w -> w.tasks().stream()).anyMatch(t -> t.id().equals(taskId));
    }
}
