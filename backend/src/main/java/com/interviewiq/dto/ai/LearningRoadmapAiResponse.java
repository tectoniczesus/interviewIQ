package com.interviewiq.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.interviewiq.dto.RoadmapContent;
import com.interviewiq.exception.InvalidAiResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Mirrors the exact JSON schema LearningRoadmapPromptBuilder instructs Groq
 * to return. Never persisted until validate() passes; toContent() then
 * assigns stable task ids (any id the model invents is ignored).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LearningRoadmapAiResponse(
        String summary,
        List<Week> weeks
) {
    public static final int MIN_WEEKS = 1;
    public static final int MAX_WEEKS = 8;
    public static final int MIN_TASKS_PER_WEEK = 1;
    public static final int MAX_TASKS_PER_WEEK = 5;
    public static final Set<String> TASK_TYPES = Set.of("LEARN", "PRACTICE", "BUILD");

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Week(
            String focus,
            List<String> skills,
            String milestone,
            List<Task> tasks
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Task(
            String title,
            String description,
            String type,
            String resourceName,
            String searchQuery
    ) {
    }

    public void validate() {
        requireText(summary, "summary");
        if (weeks == null || weeks.size() < MIN_WEEKS || weeks.size() > MAX_WEEKS) {
            throw new InvalidAiResponseException(
                    "AI response field 'weeks' must contain between " + MIN_WEEKS + " and " + MAX_WEEKS + " entries");
        }
        for (int i = 0; i < weeks.size(); i++) {
            Week week = weeks.get(i);
            String prefix = "weeks[" + i + "]";
            if (week == null) {
                throw new InvalidAiResponseException("AI response field '" + prefix + "' is null");
            }
            requireText(week.focus(), prefix + ".focus");
            if (week.skills() == null) {
                throw new InvalidAiResponseException("AI response is missing required field '" + prefix + ".skills'");
            }
            if (week.tasks() == null || week.tasks().size() < MIN_TASKS_PER_WEEK
                    || week.tasks().size() > MAX_TASKS_PER_WEEK) {
                throw new InvalidAiResponseException("AI response field '" + prefix + ".tasks' must contain between "
                        + MIN_TASKS_PER_WEEK + " and " + MAX_TASKS_PER_WEEK + " entries");
            }
            for (int j = 0; j < week.tasks().size(); j++) {
                Task task = week.tasks().get(j);
                String taskPrefix = prefix + ".tasks[" + j + "]";
                if (task == null) {
                    throw new InvalidAiResponseException("AI response field '" + taskPrefix + "' is null");
                }
                requireText(task.title(), taskPrefix + ".title");
                requireText(task.searchQuery(), taskPrefix + ".searchQuery");
            }
        }
    }

    /** Call only after validate(). Assigns ids "w{week}-t{n}" and normalizes optional fields. */
    public RoadmapContent toContent(int hoursPerWeek) {
        List<RoadmapContent.Week> contentWeeks = new ArrayList<>();
        for (int i = 0; i < weeks.size(); i++) {
            Week week = weeks.get(i);
            int weekNumber = i + 1;
            List<RoadmapContent.Task> tasks = new ArrayList<>();
            for (int j = 0; j < week.tasks().size(); j++) {
                Task task = week.tasks().get(j);
                tasks.add(new RoadmapContent.Task(
                        "w" + weekNumber + "-t" + (j + 1),
                        task.title().trim(),
                        orEmpty(task.description()),
                        normalizeType(task.type()),
                        orEmpty(task.resourceName()),
                        task.searchQuery().trim()
                ));
            }
            List<String> skills = week.skills().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .toList();
            contentWeeks.add(new RoadmapContent.Week(
                    weekNumber, week.focus().trim(), skills, orEmpty(week.milestone()), tasks));
        }
        return new RoadmapContent(summary.trim(), hoursPerWeek, contentWeeks);
    }

    private static String normalizeType(String type) {
        if (type == null) return "LEARN";
        String upper = type.trim().toUpperCase(Locale.ROOT);
        return TASK_TYPES.contains(upper) ? upper : "LEARN";
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidAiResponseException("AI response is missing required field '" + field + "'");
        }
    }
}
