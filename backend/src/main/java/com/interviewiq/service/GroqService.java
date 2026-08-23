package com.interviewiq.service;

/**
 * Low-level client for the Groq LLM API. This service only knows how to
 * send a system+user prompt and return the raw text response - it does
 * NOT know about resumes, jobs, or JSON schemas. Feature-specific prompt
 * construction lives in dedicated prompt builders (ResumeAnalysisPromptBuilder,
 * JobMatchPromptBuilder, CoverLetterPromptBuilder, InterviewPromptBuilder),
 * and feature-specific JSON validation lives in the calling service
 * (ResumeAnalysisService, JobMatchingService, etc.) - added in later phases.
 */
public interface GroqService {

    /**
     * @param systemPrompt instructions describing the task and required output format
     * @param userPrompt   the structured input (resume text, job description, etc.)
     * @return raw text content returned by the model
     */
    String complete(String systemPrompt, String userPrompt);

    /**
     * Same as complete(), but does not force JSON-object response mode.
     * Use this for free-form prose generation (e.g. cover letters) rather
     * than structured data extraction that needs schema validation.
     */
    String completeText(String systemPrompt, String userPrompt);
}