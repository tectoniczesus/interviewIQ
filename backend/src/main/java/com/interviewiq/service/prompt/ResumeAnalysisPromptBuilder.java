package com.interviewiq.service.prompt;

import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisPromptBuilder {

    private static final int MAX_RESUME_CHARS = 12000;

    public String buildSystemPrompt() {
        return """
                You are an ATS (Applicant Tracking System) resume analysis engine used inside a career-coaching product.

                Analyze the resume text the user provides and return ONLY a single JSON object - no markdown, no code fences, no commentary before or after it - matching EXACTLY this schema:

                {
                  "atsScore": <integer 0-100>,
                  "overallScore": <integer 0-100>,
                  "strengths": [<string>, ...],
                  "weaknesses": [<string>, ...],
                  "missingSkills": [<string>, ...],
                  "formattingIssues": [<string>, ...],
                  "keywordSuggestions": [<string>, ...],
                  "improvementSuggestions": [<string>, ...]
                }

                Scoring guidance:
                - atsScore reflects how well an automated applicant tracking system would parse and rank this resume (keyword usage, section structure, formatting, readability).
                - overallScore reflects overall resume quality (clarity, quantifiable achievements, relevance, structure).

                Critical rules - factual accuracy matters more than making the candidate look good:
                - Base every observation strictly on the resume text provided. Do not invent skills, employers, job titles, education, certifications, or experience that are not present in the text.
                - If information needed to judge something is missing from the resume, say so in your feedback rather than assuming it.
                - Arrays may be empty ([]) if there is genuinely nothing to report for that category, but every field must be present.
                - Do not wrap the JSON in markdown code fences. Return raw JSON only.
                """;
    }

    public String buildUserPrompt(String resumeText) {
        String trimmed = resumeText.length() > MAX_RESUME_CHARS
                ? resumeText.substring(0, MAX_RESUME_CHARS) + "\n\n[resume truncated for length]"
                : resumeText;

        return "Resume text:\n\n" + trimmed;
    }

    public String buildRetryUserPrompt(String resumeText) {
        return buildUserPrompt(resumeText)
                + "\n\nYour previous response was invalid or did not match the required JSON schema exactly. "
                + "Return ONLY valid JSON matching the schema, with no extra text.";
    }
}