package com.interviewiq.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.RoadmapTestData;
import com.interviewiq.dto.LearningRoadmapResponse;
import com.interviewiq.dto.RoadmapContent;
import com.interviewiq.dto.ai.LearningRoadmapAiResponse;
import com.interviewiq.entity.JobMatch;
import com.interviewiq.entity.LearningRoadmap;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LearningRoadmapMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LearningRoadmapMapper mapper = new LearningRoadmapMapper(objectMapper);

    private LearningRoadmap roadmap(String completedIdsJson) throws Exception {
        RoadmapContent content = objectMapper.readValue(RoadmapTestData.VALID_ROADMAP_JSON, LearningRoadmapAiResponse.class)
                .toContent(6);
        JobMatch match = RoadmapTestData.ownedMatch();
        LearningRoadmap roadmap = new LearningRoadmap();
        roadmap.setId(UUID.randomUUID());
        roadmap.setJobMatch(match);
        roadmap.setRoadmapJson(objectMapper.writeValueAsString(content));
        roadmap.setTotalTasks(content.totalTasks());
        roadmap.setCompletedTaskIds(completedIdsJson);
        return roadmap;
    }

    @Test
    void mapsTasksWithCompletionFlagsAndProgress() throws Exception {
        LearningRoadmapResponse response = mapper.toResponse(roadmap("[\"w1-t1\"]"));

        assertThat(response.jobTitle()).isEqualTo("Backend Developer");
        assertThat(response.company()).isEqualTo("Acme");
        assertThat(response.resumeFileName()).isEqualTo("resume.pdf");
        assertThat(response.matchScore()).isEqualTo(62);
        assertThat(response.hoursPerWeek()).isEqualTo(6);
        assertThat(response.weeks()).hasSize(2);
        assertThat(response.weeks().get(0).tasks())
                .extracting(LearningRoadmapResponse.TaskResponse::completed)
                .containsExactly(true, false);
        assertThat(response.totalTasks()).isEqualTo(3);
        assertThat(response.completedTasks()).isEqualTo(1);
        assertThat(response.progressPercent()).isEqualTo(33);
    }

    @Test
    void ignoresStaleTaskIdsSoProgressNeverExceedsHundred() throws Exception {
        LearningRoadmapResponse response = mapper.toResponse(
                roadmap("[\"w1-t1\",\"w1-t2\",\"w2-t1\",\"w7-t1\",\"w8-t4\"]"));

        assertThat(response.completedTasks()).isEqualTo(3);
        assertThat(response.progressPercent()).isEqualTo(100);
    }

    @Test
    void summaryCarriesWeekCountAndProgress() throws Exception {
        var summary = mapper.toSummary(roadmap("[\"w2-t1\"]"));

        assertThat(summary.weekCount()).isEqualTo(2);
        assertThat(summary.completedTasks()).isEqualTo(1);
        assertThat(summary.progressPercent()).isEqualTo(33);
    }
}
