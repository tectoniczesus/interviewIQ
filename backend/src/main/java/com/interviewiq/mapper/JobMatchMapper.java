package com.interviewiq.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.dto.JobMatchResponse;
import com.interviewiq.dto.ai.JobMatchAiResponse;
import com.interviewiq.entity.JobMatch;
import org.springframework.stereotype.Component;

@Component
public class JobMatchMapper {

    private final ObjectMapper objectMapper;

    public JobMatchMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JobMatchResponse toResponse(JobMatch match) {
        JobMatchAiResponse parsed;
        try {
            parsed = objectMapper.readValue(match.getAnalysisJson(), JobMatchAiResponse.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Stored match JSON is corrupt for match " + match.getId(), ex);
        }

        return new JobMatchResponse(
                match.getId(),
                match.getResume().getId(),
                match.getJob().getId(),
                match.getMatchScore(),
                parsed.matchingSkills(),
                parsed.missingSkills(),
                parsed.matchingExperience(),
                parsed.missingRequirements(),
                parsed.keywordGaps(),
                parsed.recommendations(),
                match.getCreatedAt()
        );
    }
}