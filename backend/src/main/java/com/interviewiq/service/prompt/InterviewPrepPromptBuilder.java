package com.interviewiq.service.prompt;

import org.springframework.stereotype.Component;

@Component
public class InterviewPrepPromptBuilder {

    private static final int MAX_RESUME_CHARS = 12000;
    private static final int MAX_JOB_CHARS = 8000;

    public String buildSystemPrompt() {
        return """
                You are an interview preparation engine used inside a career-coaching product.

                Generate interview questions for the candidate based on their resume, the target job description, and the target role. Return ONLY a single JSON object - no markdown, no code fences, no commentary before or after it - matching EXACTLY this schema:

                {
                  "questions": [
                    {
                      "question": "<string>",
                      "category": "TECHNICAL" | "HR" | "RESUME_BASED" | "JOB_SPECIFIC",
                      "difficulty": "EASY" | "MEDIUM" | "HARD",
                      "suggestedAnswer": "<string>",
                      "keyPoints": [<string>, ...]
                    },
                    ...
                  ]
                }

                Generate exactly 3 questions for EACH of the 4 categories (12 questions total):
                - TECHNICAL: programming, data structures, algorithms, databases, frameworks, system design, or technologies mentioned in the resume/job description. Mix of difficulty levels.
                - HR: standard behavioral/HR questions (e.g. tell me about yourself, why this role, strengths/weaknesses, why should we hire you).
                - RESUME_BASED: questions that specifically reference projects, employers, technologies, or experiences that are ACTUALLY present in the candidate's resume text.
                - JOB_SPECIFIC: questions grounded in the specific requirements and responsibilities from the job description.

                For each question, suggestedAnswer should be a realistic, well-structured example answer the candidate could give, and keyPoints should be 3-5 short bullet phrases (not full sentences) capturing what a strong answer should hit.

                Critical rules - factual accuracy matters more than making the candidate look impressive:
                - RESUME_BASED questions and suggested answers must only reference projects, employers, technologies, and experience that are explicitly present in the supplied resume text. Never invent experience the candidate doesn't have.
                - Do not wrap the JSON in markdown code fences. Return raw JSON only.
                """;
    }

    public String buildUserPrompt(String resumeText, String jobDescription, String targetRole) {
        return "Target role: " + targetRole
                + "\n\nResume text:\n\n" + truncate(resumeText, MAX_RESUME_CHARS)
                + "\n\n---\n\nJob description:\n\n" + truncate(jobDescription, MAX_JOB_CHARS);
    }

    public String buildRetryUserPrompt(String resumeText, String jobDescription, String targetRole) {
        return buildUserPrompt(resumeText, jobDescription, targetRole)
                + "\n\nYour previous response was invalid or did not match the required JSON schema exactly. "
                + "Return ONLY valid JSON matching the schema, with no extra text.";
    }

    private String truncate(String text, int maxChars) {
        return text.length() > maxChars
                ? text.substring(0, maxChars) + "\n\n[truncated for length]"
                : text;
    }
}