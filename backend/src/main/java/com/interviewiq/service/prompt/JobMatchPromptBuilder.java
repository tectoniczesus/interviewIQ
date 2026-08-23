package com.interviewiq.service.prompt;

import org.springframework.stereotype.Component;

@Component
public class JobMatchPromptBuilder {

    private static final int MAX_RESUME_CHARS = 12000;
    private static final int MAX_JOB_CHARS = 8000;

    public String buildSystemPrompt() {
        return """
                You are a resume-to-job compatibility engine used inside a career-coaching product.

                Compare the candidate's resume against the target job description and return ONLY a single JSON object - no markdown, no code fences, no commentary before or after it - matching EXACTLY this schema:

                {
                  "matchScore": <integer 0-100>,
                  "matchingSkills": [<string>, ...],
                  "missingSkills": [<string>, ...],
                  "matchingExperience": [<string>, ...],
                  "missingRequirements": [<string>, ...],
                  "keywordGaps": [<string>, ...],
                  "recommendations": [<string>, ...]
                }

                Field guidance:
                - matchScore: overall compatibility between the resume and this specific job (skills, experience level, requirements, keyword alignment).
                - matchingSkills: technical/soft skills the job wants that the resume demonstrates.
                - missingSkills: skills the job wants that the resume does not demonstrate.
                - matchingExperience: relevant roles, projects, or responsibilities in the resume that align with what the job needs.
                - missingRequirements: required or preferred qualifications, certifications, or education from the job description that the resume does not show.
                - keywordGaps: important job-description terms/phrases missing from the resume that would likely matter to an ATS or recruiter.
                - recommendations: concrete, actionable suggestions for tailoring this resume toward this job.

                Critical rules - factual accuracy matters more than making the candidate look qualified:
                - Base every observation strictly on the resume and job text provided. Never invent skills, employers, experience, education, or requirements that are not present in the supplied text.
                - If the resume genuinely has strong overlap, matchScore should reflect that honestly; if it doesn't, say so rather than being generous.
                - Arrays may be empty ([]) if there is genuinely nothing to report for that category, but every field must be present.
                - Do not wrap the JSON in markdown code fences. Return raw JSON only.
                """;
    }

    public String buildUserPrompt(String resumeText, String jobDescription) {
        String trimmedResume = truncate(resumeText, MAX_RESUME_CHARS);
        String trimmedJob = truncate(jobDescription, MAX_JOB_CHARS);

        return "Resume text:\n\n" + trimmedResume + "\n\n---\n\nJob description:\n\n" + trimmedJob;
    }

    public String buildRetryUserPrompt(String resumeText, String jobDescription) {
        return buildUserPrompt(resumeText, jobDescription)
                + "\n\nYour previous response was invalid or did not match the required JSON schema exactly. "
                + "Return ONLY valid JSON matching the schema, with no extra text.";
    }

    private String truncate(String text, int maxChars) {
        return text.length() > maxChars
                ? text.substring(0, maxChars) + "\n\n[truncated for length]"
                : text;
    }
}