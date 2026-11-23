package com.interviewiq.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * A week-by-week skill-gap learning plan generated from one JobMatch.
 * One roadmap per match: regenerating replaces the plan and resets progress.
 */
@Entity
@Table(name = "learning_roadmaps")
public class LearningRoadmap extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_match_id", nullable = false, unique = true)
    private JobMatch jobMatch;

    @Column(name = "target_role")
    private String targetRole;

    /** Serialized {@link com.interviewiq.dto.RoadmapContent} (summary, hoursPerWeek, weeks with server-assigned task ids). */
    @Column(name = "roadmap_json", nullable = false, columnDefinition = "TEXT")
    private String roadmapJson;

    /** JSON array of completed task ids, e.g. ["w1-t1","w2-t3"]. */
    @Column(name = "completed_task_ids", nullable = false, columnDefinition = "TEXT")
    private String completedTaskIds = "[]";

    @Column(name = "total_tasks", nullable = false)
    private Integer totalTasks;

    public JobMatch getJobMatch() {
        return jobMatch;
    }

    public void setJobMatch(JobMatch jobMatch) {
        this.jobMatch = jobMatch;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getRoadmapJson() {
        return roadmapJson;
    }

    public void setRoadmapJson(String roadmapJson) {
        this.roadmapJson = roadmapJson;
    }

    public String getCompletedTaskIds() {
        return completedTaskIds;
    }

    public void setCompletedTaskIds(String completedTaskIds) {
        this.completedTaskIds = completedTaskIds;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }
}
