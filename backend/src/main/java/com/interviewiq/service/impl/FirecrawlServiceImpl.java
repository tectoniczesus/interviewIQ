package com.interviewiq.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.exception.FirecrawlApiException;
import com.interviewiq.service.FirecrawlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

@Service
public class FirecrawlServiceImpl implements FirecrawlService {

    private static final Logger log = LoggerFactory.getLogger(FirecrawlServiceImpl.class);

    private final WebClient firecrawlClient;
    private final ObjectMapper objectMapper;

    public FirecrawlServiceImpl(@Qualifier("firecrawlWebClient") WebClient firecrawlClient,
                                ObjectMapper objectMapper) {
        this.firecrawlClient = firecrawlClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String scrapeUrl(String url) {
        Map<String, Object> requestBody = Map.of(
                "url", url,
                "formats", new String[] { "markdown" }
        );

        try {
            JsonNode response = firecrawlClient.post()
                    .uri("/scrape")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null || !response.path("success").asBoolean(false)) {
                throw new FirecrawlApiException("Firecrawl was unable to extract content from this URL");
            }

            String markdown = response.path("data").path("markdown").asText("");
            if (markdown.isBlank()) {
                throw new FirecrawlApiException("No extractable content found at this URL");
            }

            return markdown;
        } catch (FirecrawlApiException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            // Firecrawl often explains *why* in the response body (target site blocked
            // the scraper, domain unsupported, etc.) - surface that instead of a bare
            // "403 Forbidden" that hides the real reason.
            String reason = extractReason(ex.getResponseBodyAsString());
            log.warn("Firecrawl returned {} for url={} - body={}", ex.getStatusCode(), url, ex.getResponseBodyAsString());
            throw new FirecrawlApiException(
                    "Firecrawl could not access this URL (" + ex.getStatusCode() + "): " + reason, ex);
        } catch (Exception ex) {
            throw new FirecrawlApiException("Failed to call the Firecrawl API", ex);
        }
    }

    private String extractReason(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "no further detail returned";
        }
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            String error = node.path("error").asText(null);
            return error != null ? error : responseBody;
        } catch (Exception parseEx) {
            return responseBody;
        }
    }
}