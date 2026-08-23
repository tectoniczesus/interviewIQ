package com.interviewiq.service.prompt;

import org.springframework.stereotype.Component;

@Component
public class CoverLetterPromptBuilder {

    private static final int MAX_RESUME_CHARS = 12000;
    private static final int MAX_JOB_CHARS = 8000;

    public String buildSystemPrompt() {
        return """
                You are a professional cover letter writer used inside a career-coaching product.

                Write a personalized, one-page cover letter for the candidate, tailored to the specific job provided. Return ONLY the cover letter text itself - no markdown, no headers like "Cover Letter:", no commentary before or after it, no subject line.

                The letter should:
                - Open with a professional greeting. If a hiring manager or company name is available, use it; otherwise use a generic but professional greeting like "Dear Hiring Team,".
                - Reference specific, real experience, projects, and skills that are actually present in the candidate's resume and are relevant to this job.
                - Explain concretely why the candidate is a fit for THIS job, connecting their background to the job's actual requirements - not generic statements that could apply to any job.
                - Avoid generic filler phrases ("I am a hard worker", "I am a team player") unless backed by a specific example from the resume.
                - Stay to 3-4 paragraphs, professional tone, ready to send as-is.
                - Close with a professional sign-off.

                Critical rules - factual accuracy matters more than making the candidate look impressive:
                - Only reference companies, job titles, skills, projects, and achievements that are explicitly present in the supplied resume text. Never invent or embellish experience.
                - If the candidate's name is provided, sign the letter with that name. If it is not provided and is not clearly stated in the resume text, close with "Sincerely," and no name (do not invent one).
                - Do not fabricate a company name for the candidate to invent enthusiasm about if the job description doesn't name one - refer to "your team" or "your organization" instead.
                """;
    }

    public String buildUserPrompt(String resumeText, String jobDescription, String jobTitle, String company, String candidateName) {
        StringBuilder sb = new StringBuilder();

        if (candidateName != null && !candidateName.isBlank()) {
            sb.append("Candidate name: ").append(candidateName.trim()).append("\n\n");
        }
        if (jobTitle != null && !jobTitle.isBlank()) {
            sb.append("Job title: ").append(jobTitle.trim()).append("\n");
        }
        if (company != null && !company.isBlank()) {
            sb.append("Company: ").append(company.trim()).append("\n");
        }

        sb.append("\nResume text:\n\n").append(truncate(resumeText, MAX_RESUME_CHARS));
        sb.append("\n\n---\n\nJob description:\n\n").append(truncate(jobDescription, MAX_JOB_CHARS));

        return sb.toString();
    }

    private String truncate(String text, int maxChars) {
        return text.length() > maxChars
                ? text.substring(0, maxChars) + "\n\n[truncated for length]"
                : text;
    }
}