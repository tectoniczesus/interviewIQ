package com.interviewiq.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.RoadmapTestData;
import com.interviewiq.dto.RoadmapContent;
import com.interviewiq.entity.JobMatch;
import com.interviewiq.entity.LearningRoadmap;
import com.interviewiq.exception.InvalidAiResponseException;
import com.interviewiq.exception.ResourceNotFoundException;
import com.interviewiq.exception.UnauthorizedResourceException;
import com.interviewiq.exception.ValidationException;
import com.interviewiq.repository.LearningRoadmapRepository;
import com.interviewiq.security.AuthenticatedUser;
import com.interviewiq.service.GroqService;
import com.interviewiq.service.JobMatchingService;
import com.interviewiq.service.prompt.LearningRoadmapPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningRoadmapServiceImplTest {

    private static final AuthenticatedUser OWNER = new AuthenticatedUser(RoadmapTestData.OWNER_CLERK_ID, "o@example.com");
    private static final AuthenticatedUser STRANGER = new AuthenticatedUser("user_stranger", "s@example.com");

    @Mock
    private JobMatchingService jobMatchingService;
    @Mock
    private LearningRoadmapRepository roadmapRepository;
    @Mock
    private GroqService groqService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LearningRoadmapPromptBuilder promptBuilder = new LearningRoadmapPromptBuilder();
    private LearningRoadmapServiceImpl service;
    private JobMatch match;

    @BeforeEach
    void setUp() {
        service = new LearningRoadmapServiceImpl(jobMatchingService, roadmapRepository, groqService, promptBuilder, objectMapper);
        match = RoadmapTestData.ownedMatch();
    }

    private void stubOwnedMatch() {
        when(jobMatchingService.getByIdForUser(match.getId(), OWNER)).thenReturn(match);
    }

    private void stubSavePassThrough() {
        when(roadmapRepository.save(any(LearningRoadmap.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private LearningRoadmap existingRoadmap(String completedIdsJson) throws Exception {
        var parsed = objectMapper.readValue(RoadmapTestData.VALID_ROADMAP_JSON,
                com.interviewiq.dto.ai.LearningRoadmapAiResponse.class);
        RoadmapContent content = parsed.toContent(8);
        LearningRoadmap roadmap = new LearningRoadmap();
        roadmap.setId(UUID.randomUUID());
        roadmap.setJobMatch(match);
        roadmap.setRoadmapJson(objectMapper.writeValueAsString(content));
        roadmap.setTotalTasks(content.totalTasks());
        roadmap.setCompletedTaskIds(completedIdsJson);
        return roadmap;
    }

    private List<String> completedIds(LearningRoadmap roadmap) throws Exception {
        return objectMapper.readValue(roadmap.getCompletedTaskIds(), new TypeReference<>() { });
    }

    // --- generate ---

    @Test
    void generateBuildsPromptFromMatchGapsAndPersistsValidatedRoadmap() {
        stubOwnedMatch();
        stubSavePassThrough();
        when(roadmapRepository.findByJobMatchId(match.getId())).thenReturn(Optional.empty());
        when(groqService.complete(anyString(), anyString())).thenReturn(RoadmapTestData.VALID_ROADMAP_JSON);

        LearningRoadmap result = service.generateForMatch(match.getId(), 12, OWNER);

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(groqService, times(1)).complete(anyString(), userPrompt.capture());
        assertThat(userPrompt.getValue())
                .contains("Backend Developer at Acme")
                .contains("- Docker")
                .contains("- CI/CD experience")
                .contains("- microservices")
                .contains("12 hours per week")
                .contains("62/100");

        assertThat(result.getJobMatch()).isSameAs(match);
        assertThat(result.getTargetRole()).isEqualTo("Backend Developer");
        assertThat(result.getTotalTasks()).isEqualTo(3);
        assertThat(result.getCompletedTaskIds()).isEqualTo("[]");
        RoadmapContent saved = service.readContent(result);
        assertThat(saved.hoursPerWeek()).isEqualTo(12);
        assertThat(saved.weeks().get(0).tasks().get(0).id()).isEqualTo("w1-t1");
    }

    @Test
    void generateDefaultsToEightHoursWhenNotProvided() {
        stubOwnedMatch();
        stubSavePassThrough();
        when(roadmapRepository.findByJobMatchId(match.getId())).thenReturn(Optional.empty());
        when(groqService.complete(anyString(), contains("8 hours per week"))).thenReturn(RoadmapTestData.VALID_ROADMAP_JSON);

        LearningRoadmap result = service.generateForMatch(match.getId(), null, OWNER);

        assertThat(service.readContent(result).hoursPerWeek()).isEqualTo(8);
    }

    @Test
    void generateRetriesOnceWithRetryPromptThenSucceeds() {
        stubOwnedMatch();
        stubSavePassThrough();
        when(roadmapRepository.findByJobMatchId(match.getId())).thenReturn(Optional.empty());
        when(groqService.complete(anyString(), anyString()))
                .thenReturn("{\"summary\":\"missing weeks\"}")
                .thenReturn(RoadmapTestData.VALID_ROADMAP_JSON);

        LearningRoadmap result = service.generateForMatch(match.getId(), 8, OWNER);

        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
        verify(groqService, times(2)).complete(anyString(), prompts.capture());
        assertThat(prompts.getAllValues().get(0)).doesNotContain("previous response was invalid");
        assertThat(prompts.getAllValues().get(1)).contains("previous response was invalid");
        assertThat(result.getTotalTasks()).isEqualTo(3);
    }

    @Test
    void generateThrowsAfterMaxAttemptsAndSavesNothing() {
        stubOwnedMatch();
        when(groqService.complete(anyString(), anyString())).thenReturn("not json at all");

        assertThatThrownBy(() -> service.generateForMatch(match.getId(), 8, OWNER))
                .isInstanceOf(InvalidAiResponseException.class);

        verify(groqService, times(LearningRoadmapServiceImpl.MAX_ATTEMPTS)).complete(anyString(), anyString());
        verify(roadmapRepository, never()).save(any());
    }

    @Test
    void regenerateReplacesExistingRoadmapAndResetsProgress() throws Exception {
        LearningRoadmap existing = existingRoadmap("[\"w1-t1\",\"w2-t1\"]");
        stubOwnedMatch();
        stubSavePassThrough();
        when(roadmapRepository.findByJobMatchId(match.getId())).thenReturn(Optional.of(existing));
        when(groqService.complete(anyString(), anyString())).thenReturn(RoadmapTestData.VALID_ROADMAP_JSON);

        LearningRoadmap result = service.generateForMatch(match.getId(), 8, OWNER);

        assertThat(result).isSameAs(existing); // same row updated, not a second row (unique job_match_id)
        assertThat(result.getCompletedTaskIds()).isEqualTo("[]");
    }

    @Test
    void generateRejectsOutOfRangeHoursBeforeCallingAi() {
        stubOwnedMatch();

        assertThatThrownBy(() -> service.generateForMatch(match.getId(), 1, OWNER)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.generateForMatch(match.getId(), 41, OWNER)).isInstanceOf(ValidationException.class);
        verify(groqService, never()).complete(anyString(), anyString());
    }

    @Test
    void generatePropagatesOwnershipFailureFromMatchService() {
        when(jobMatchingService.getByIdForUser(match.getId(), STRANGER))
                .thenThrow(new UnauthorizedResourceException("You do not have access to this job match"));

        assertThatThrownBy(() -> service.generateForMatch(match.getId(), 8, STRANGER))
                .isInstanceOf(UnauthorizedResourceException.class);
        verify(groqService, never()).complete(anyString(), anyString());
    }

    // --- read ---

    @Test
    void getForMatchThrowsNotFoundWhenNoRoadmapYet() {
        stubOwnedMatch();
        when(roadmapRepository.findByJobMatchId(match.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForMatch(match.getId(), OWNER)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdRejectsOtherUsers() throws Exception {
        LearningRoadmap roadmap = existingRoadmap("[]");
        when(roadmapRepository.findWithDetailsById(roadmap.getId())).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> service.getByIdForUser(roadmap.getId(), STRANGER))
                .isInstanceOf(UnauthorizedResourceException.class);
        assertThat(service.getByIdForUser(roadmap.getId(), OWNER)).isSameAs(roadmap);
    }

    // --- progress ---

    @Test
    void setTaskCompletedAddsAndRemovesTaskIds() throws Exception {
        LearningRoadmap roadmap = existingRoadmap("[]");
        when(roadmapRepository.findByIdForUpdate(roadmap.getId())).thenReturn(Optional.of(roadmap));
        stubSavePassThrough();

        service.setTaskCompleted(roadmap.getId(), "w1-t2", true, OWNER);
        service.setTaskCompleted(roadmap.getId(), "w2-t1", true, OWNER);
        assertThat(completedIds(roadmap)).containsExactly("w1-t2", "w2-t1");

        service.setTaskCompleted(roadmap.getId(), "w1-t2", false, OWNER);
        assertThat(completedIds(roadmap)).containsExactly("w2-t1");
    }

    @Test
    void setTaskCompletedIsIdempotentAndSkipsSaveWhenNothingChanged() throws Exception {
        LearningRoadmap roadmap = existingRoadmap("[\"w1-t1\"]");
        when(roadmapRepository.findByIdForUpdate(roadmap.getId())).thenReturn(Optional.of(roadmap));

        service.setTaskCompleted(roadmap.getId(), "w1-t1", true, OWNER);

        assertThat(completedIds(roadmap)).containsExactly("w1-t1");
        verify(roadmapRepository, never()).save(any());
    }

    @Test
    void setTaskCompletedRejectsUnknownTaskId() throws Exception {
        LearningRoadmap roadmap = existingRoadmap("[]");
        when(roadmapRepository.findByIdForUpdate(roadmap.getId())).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> service.setTaskCompleted(roadmap.getId(), "w9-t9", true, OWNER))
                .isInstanceOf(ValidationException.class);
        verify(roadmapRepository, never()).save(any());
    }

    @Test
    void setTaskCompletedRejectsOtherUsers() throws Exception {
        LearningRoadmap roadmap = existingRoadmap("[]");
        when(roadmapRepository.findByIdForUpdate(roadmap.getId())).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> service.setTaskCompleted(roadmap.getId(), "w1-t1", true, STRANGER))
                .isInstanceOf(UnauthorizedResourceException.class);
        verify(roadmapRepository, never()).save(any());
    }

    @Test
    void deleteRejectsOtherUsersAndDeletesForOwner() throws Exception {
        LearningRoadmap roadmap = existingRoadmap("[]");
        when(roadmapRepository.findWithDetailsById(roadmap.getId())).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> service.deleteForUser(roadmap.getId(), STRANGER))
                .isInstanceOf(UnauthorizedResourceException.class);
        verify(roadmapRepository, never()).delete(any());

        service.deleteForUser(roadmap.getId(), OWNER);
        verify(roadmapRepository).delete(roadmap);
    }
}
