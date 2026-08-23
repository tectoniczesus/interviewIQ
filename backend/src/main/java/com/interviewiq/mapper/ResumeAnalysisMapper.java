package com.interviewiq.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.dto.ResumeAnalysisResponse;
import com.interviewiq.dto.ai.ResumeAnalysisAiResponse;
import com.interviewiq.entity.ResumeAnalysis;
import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisMapper {

    private final ObjectMapper objectMapper;

    public ResumeAnalysisMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ResumeAnalysisResponse toResponse(ResumeAnalysis analysis) {
        ResumeAnalysisAiResponse parsed;
        try {
            parsed = objectMapper.readValue(analysis.getAnalysisJson(), ResumeAnalysisAiResponse.class);
        } catch (Exception ex) {
            // Should be unreachable - we validate before ever persisting analysisJson -
            // but fail loudly rather than silently return a broken response if it happens.
            throw new IllegalStateException("Stored analysis JSON is corrupt for analysis " + analysis.getId(), ex);
        }

        return new ResumeAnalysisResponse(
                analysis.getId(),
                analysis.getResume().getId(),
                analysis.getAtsScore(),
                analysis.getOverallScore(),
                parsed.strengths(),
                parsed.weaknesses(),
                parsed.missingSkills(),
                parsed.formattingIssues(),
                parsed.keywordSuggestions(),
                parsed.improvementSuggestions(),
                analysis.getCreatedAt()
        );
    }
}