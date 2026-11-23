package com.interviewiq.service.prompt;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LearningRoadmapPromptBuilder {

    private static final int MAX_JOB_CHARS = 4000;
    private static final int MAX_LIST_ITEMS = 15;

    public String buildSystemPrompt() {
        return """
                You are a career learning coach inside a job-preparation product for students and early-career developers.

                Given a target job and the gaps found between the candidate's resume and that job, design a realistic week-by-week learning roadmap that closes the most important gaps first. Return ONLY a single JSON object - no markdown, no code fences, no commentary - matching EXACTLY this schema:

                {
                  "summary": "<2-3 sentences: what this plan covers and why, in second person>",
                  "weeks": [
                    {
                      "focus": "<short theme for the week, e.g. 'Docker fundamentals'>",
                      "skills": ["<skill covered>", ...],
                      "milestone": "<one sentence: what the candidate can show or do by the end of the week>",
                      "tasks": [
                        {
                          "title": "<short actionable task>",
                          "description": "<one sentence on what to do and why it matters for this job>",
                          "type": "LEARN" | "PRACTICE" | "BUILD",
                          "resourceName": "<name of a FREE resource, from the list below>",
                          "searchQuery": "<a short search query that finds this exact material, e.g. 'docker compose tutorial for beginners'>"
                        }
                      ]
                    }
                  ]
                }

                Rules:
                - Between 3 and 8 weeks, depending on how large the gaps are. Each week has 2 to 4 tasks. Keep every description to one sentence.
                - Fit each week into the stated hours per week.
                - Order by impact: required/core skills for the job first, nice-to-haves later. The final week should include a BUILD task (a small portfolio project that uses the new skills) and interview practice for the role.
                - Only recommend FREE resources. Use resourceName values such as: Official Documentation, MDN Web Docs, freeCodeCamp, The Odin Project, roadmap.sh, CS50 (Harvard, free), Khan Academy, Kaggle Learn, GeeksforGeeks, W3Schools, Exercism, LeetCode (free problems), HackerRank, YouTube, GitHub. Never recommend paid courses, bootcamps, or paid certifications.
                - NEVER include URLs or links anywhere. The product builds search links from resourceName and searchQuery.
                - Do not teach skills the candidate already has (listed as matching skills) except as brief context.
                - If the gap lists are empty, build a shorter plan that deepens the job's core skills and prepares for interviews.
                - Return raw JSON only.
                """;
    }

    public String buildUserPrompt(String jobTitle,
                                  String company,
                                  String jobDescription,
                                  int matchScore,
                                  List<String> matchingSkills,
                                  List<String> missingSkills,
                                  List<String> missingRequirements,
                                  List<String> keywordGaps,
                                  int hoursPerWeek) {
        StringBuilder sb = new StringBuilder();
        sb.append("Target role: ").append(blankTo(jobTitle, "Not specified"));
        if (company != null && !company.isBlank()) {
            sb.append(" at ").append(company.trim());
        }
        sb.append("\nCurrent resume-to-job match score: ").append(matchScore).append("/100");
        sb.append("\nAvailable study time: ").append(hoursPerWeek).append(" hours per week");
        sb.append("\n\nSkills the candidate already has (matching skills):\n").append(bullets(matchingSkills));
        sb.append("\n\nMissing skills:\n").append(bullets(missingSkills));
        sb.append("\n\nMissing requirements:\n").append(bullets(missingRequirements));
        sb.append("\n\nKeyword gaps:\n").append(bullets(keywordGaps));
        sb.append("\n\n---\n\nJob description (for context):\n\n").append(truncate(blankTo(jobDescription, ""), MAX_JOB_CHARS));
        return sb.toString();
    }

    public String buildRetryUserPrompt(String originalUserPrompt) {
        return originalUserPrompt
                + "\n\nYour previous response was invalid or did not match the required JSON schema exactly "
                + "(3-8 weeks, 2-4 tasks per week, every field present, no URLs). "
                + "Return ONLY valid JSON matching the schema, with no extra text.";
    }

    private static String bullets(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- (none)";
        }
        return items.stream()
                .filter(s -> s != null && !s.isBlank())
                .limit(MAX_LIST_ITEMS)
                .map(s -> "- " + s.trim())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("- (none)");
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String truncate(String text, int maxChars) {
        return text.length() > maxChars
                ? text.substring(0, maxChars) + "\n\n[truncated for length]"
                : text;
    }
}
