package com.interviewiq.service.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.config.GroqConfig;
import com.interviewiq.dto.RoadmapContent;
import com.interviewiq.dto.ai.LearningRoadmapAiResponse;
import com.interviewiq.service.GroqService;
import com.interviewiq.service.impl.GroqServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in: calls the real Groq API with the real prompt and checks the model's
 * output passes validate(). Skipped by default so `mvn test` stays offline.
 *
 *   GROQ_LIVE_TEST=true mvn test -Dtest=LearningRoadmapLiveGroqTest
 */
@EnabledIfEnvironmentVariable(named = "GROQ_LIVE_TEST", matches = "true")
@SpringBootTest(classes = LearningRoadmapLiveGroqTest.Config.class)
class LearningRoadmapLiveGroqTest {

    @Configuration
    @EnableConfigurationProperties
    @Import({GroqConfig.class, GroqServiceImpl.class, LearningRoadmapPromptBuilder.class})
    static class Config {
    }

    @Autowired
    private GroqService groqService;
    @Autowired
    private LearningRoadmapPromptBuilder promptBuilder;

    @Test
    void realModelReturnsRoadmapThatPassesValidation() throws Exception {
        String userPrompt = promptBuilder.buildUserPrompt(
                "Backend Developer",
                "Acme",
                "We are hiring a backend developer with Java, Spring Boot, Docker, Kubernetes, "
                        + "CI/CD pipelines (GitHub Actions) and experience designing microservices on AWS.",
                58,
                List.of("Java", "Spring Boot", "SQL"),
                List.of("Docker", "Kubernetes", "AWS"),
                List.of("CI/CD pipeline experience"),
                List.of("microservices", "GitHub Actions"),
                8);

        String raw = groqService.complete(promptBuilder.buildSystemPrompt(), userPrompt);

        LearningRoadmapAiResponse parsed = new ObjectMapper().readValue(raw, LearningRoadmapAiResponse.class);
        parsed.validate();
        RoadmapContent content = parsed.toContent(8);

        System.out.println("[live-groq] weeks=" + content.weeks().size() + " tasks=" + content.totalTasks());
        content.weeks().forEach(w -> System.out.println("[live-groq]   week " + w.week() + ": " + w.focus()
                + " (" + w.tasks().size() + " tasks) e.g. " + w.tasks().get(0).resourceName()
                + " / \"" + w.tasks().get(0).searchQuery() + "\""));

        assertThat(content.weeks()).hasSizeBetween(1, LearningRoadmapAiResponse.MAX_WEEKS);
        assertThat(raw).as("prompt forbids URLs").doesNotContain("http://").doesNotContain("https://");
    }
}
