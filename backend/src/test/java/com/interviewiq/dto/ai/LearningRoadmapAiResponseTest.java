package com.interviewiq.dto.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.RoadmapTestData;
import com.interviewiq.dto.RoadmapContent;
import com.interviewiq.exception.InvalidAiResponseException;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearningRoadmapAiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static LearningRoadmapAiResponse.Task task(String title, String query) {
        return new LearningRoadmapAiResponse.Task(title, "desc", "LEARN", "MDN Web Docs", query);
    }

    private static LearningRoadmapAiResponse.Week week(List<LearningRoadmapAiResponse.Task> tasks) {
        return new LearningRoadmapAiResponse.Week("Focus", List.of("Skill"), "Milestone", tasks);
    }

    @Test
    void validRoadmapPassesValidation() throws Exception {
        LearningRoadmapAiResponse parsed =
                objectMapper.readValue(RoadmapTestData.VALID_ROADMAP_JSON, LearningRoadmapAiResponse.class);
        assertThatCode(parsed::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingSummary() {
        var response = new LearningRoadmapAiResponse(" ", List.of(week(List.of(task("t", "q")))));
        assertThatThrownBy(response::validate).isInstanceOf(InvalidAiResponseException.class)
                .hasMessageContaining("summary");
    }

    @Test
    void rejectsNoWeeksAndTooManyWeeks() {
        assertThatThrownBy(new LearningRoadmapAiResponse("s", List.of())::validate)
                .isInstanceOf(InvalidAiResponseException.class);
        assertThatThrownBy(new LearningRoadmapAiResponse("s", null)::validate)
                .isInstanceOf(InvalidAiResponseException.class);

        var nineWeeks = Collections.nCopies(9, week(List.of(task("t", "q"))));
        assertThatThrownBy(new LearningRoadmapAiResponse("s", nineWeeks)::validate)
                .isInstanceOf(InvalidAiResponseException.class)
                .hasMessageContaining("weeks");

        var eightWeeks = Collections.nCopies(8, week(List.of(task("t", "q"))));
        assertThatCode(new LearningRoadmapAiResponse("s", eightWeeks)::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsWeekWithNoTasksOrTooManyTasks() {
        assertThatThrownBy(new LearningRoadmapAiResponse("s", List.of(week(List.of())))::validate)
                .isInstanceOf(InvalidAiResponseException.class)
                .hasMessageContaining("weeks[0].tasks");

        var sixTasks = Collections.nCopies(6, task("t", "q"));
        assertThatThrownBy(new LearningRoadmapAiResponse("s", List.of(week(sixTasks)))::validate)
                .isInstanceOf(InvalidAiResponseException.class);
    }

    @Test
    void rejectsTaskWithoutTitleOrSearchQuery() {
        assertThatThrownBy(new LearningRoadmapAiResponse("s", List.of(week(List.of(task("", "q")))))::validate)
                .isInstanceOf(InvalidAiResponseException.class)
                .hasMessageContaining("title");
        assertThatThrownBy(new LearningRoadmapAiResponse("s", List.of(week(List.of(task("t", null)))))::validate)
                .isInstanceOf(InvalidAiResponseException.class)
                .hasMessageContaining("searchQuery");
    }

    @Test
    void rejectsWeekWithoutSkillsList() {
        var noSkills = new LearningRoadmapAiResponse.Week("Focus", null, "m", List.of(task("t", "q")));
        assertThatThrownBy(new LearningRoadmapAiResponse("s", List.of(noSkills))::validate)
                .isInstanceOf(InvalidAiResponseException.class)
                .hasMessageContaining("skills");
    }

    @Test
    void toContentAssignsStableIdsAndNormalizesTypes() throws Exception {
        LearningRoadmapAiResponse parsed =
                objectMapper.readValue(RoadmapTestData.VALID_ROADMAP_JSON, LearningRoadmapAiResponse.class);
        parsed.validate();

        RoadmapContent content = parsed.toContent(10);

        assertThat(content.hoursPerWeek()).isEqualTo(10);
        assertThat(content.totalTasks()).isEqualTo(3);
        assertThat(content.weeks()).extracting(RoadmapContent.Week::week).containsExactly(1, 2);
        assertThat(content.weeks().get(0).tasks()).extracting(RoadmapContent.Task::id)
                .containsExactly("w1-t1", "w1-t2"); // model's "model-made-up" id is ignored
        assertThat(content.weeks().get(1).tasks().get(0).id()).isEqualTo("w2-t1");

        assertThat(content.weeks().get(0).tasks().get(0).type()).isEqualTo("LEARN");     // "learn" -> upper-cased
        assertThat(content.weeks().get(1).tasks().get(0).type()).isEqualTo("LEARN");     // unknown -> default
        assertThat(content.weeks().get(0).tasks().get(1).type()).isEqualTo("PRACTICE");

        assertThat(content.hasTask("w2-t1")).isTrue();
        assertThat(content.hasTask("w3-t1")).isFalse();
    }
}
