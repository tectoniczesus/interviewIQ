package com.interviewiq.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewiq.config.GroqConfig;
import com.interviewiq.exception.GroqApiException;
import com.interviewiq.service.GroqService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class GroqServiceImpl implements GroqService {

    private final WebClient groqClient;
    private final GroqConfig groqConfig;

    public GroqServiceImpl(@Qualifier("groqWebClient") WebClient groqClient, GroqConfig groqConfig) {
        this.groqClient = groqClient;
        this.groqConfig = groqConfig;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", groqConfig.getModel(),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        return call(requestBody);
    }

    @Override
    public String completeText(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", groqConfig.getModel(),
                "temperature", 0.5,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        return call(requestBody);
    }

    private String call(Map<String, Object> requestBody) {
        try {
            JsonNode response = groqClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            if (response == null) {
                throw new GroqApiException("Groq API returned an empty response");
            }

            JsonNode choices = response.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new GroqApiException("Groq API response did not contain any choices");
            }

            return choices.get(0).path("message").path("content").asText();
        } catch (GroqApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GroqApiException("Failed to call the Groq API", ex);
        }
    }
}