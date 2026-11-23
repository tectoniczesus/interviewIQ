package com.interviewiq;

import com.interviewiq.entity.Job;
import com.interviewiq.entity.JobMatch;
import com.interviewiq.entity.Resume;
import com.interviewiq.entity.User;

import java.util.UUID;

/** Shared fixtures for the learning-roadmap tests. */
public final class RoadmapTestData {

    public static final String OWNER_CLERK_ID = "user_owner";

    public static final String MATCH_ANALYSIS_JSON = """
            {"matchScore":62,
             "matchingSkills":["Java","SQL"],
             "missingSkills":["Docker","Kubernetes"],
             "matchingExperience":["Built REST APIs"],
             "missingRequirements":["CI/CD experience"],
             "keywordGaps":["microservices"],
             "recommendations":["Add a containerized project"]}
            """;

    /** Two weeks, three tasks total; the model-supplied "id" and odd type casing must be ignored/normalized. */
    public static final String VALID_ROADMAP_JSON = """
            {"summary":"Close your container gaps first, then ship a deployed project.",
             "weeks":[
               {"focus":"Docker fundamentals","skills":["Docker"],"milestone":"Containerize a Spring app",
                "tasks":[
                  {"id":"model-made-up","title":"Learn images and containers","description":"Read the getting started guide.",
                   "type":"learn","resourceName":"Official Documentation","searchQuery":"docker getting started"},
                  {"title":"Write a Dockerfile","description":"Containerize a small API.",
                   "type":"PRACTICE","resourceName":"YouTube","searchQuery":"dockerfile spring boot tutorial"}
                ]},
               {"focus":"Kubernetes basics","skills":["Kubernetes"],"milestone":"Deploy to a local cluster",
                "tasks":[
                  {"title":"Deploy with minikube","description":"Run your container on Kubernetes.",
                   "type":"something-else","resourceName":"freeCodeCamp","searchQuery":"kubernetes minikube beginners"}
                ]}
             ]}
            """;

    private RoadmapTestData() {
    }

    public static JobMatch ownedMatch() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setClerkUserId(OWNER_CLERK_ID);

        Resume resume = new Resume();
        resume.setId(UUID.randomUUID());
        resume.setUser(user);
        resume.setFileName("resume.pdf");

        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setUser(user);
        job.setTitle("Backend Developer");
        job.setCompany("Acme");
        job.setDescription("We need Java, Docker and Kubernetes.");

        JobMatch match = new JobMatch();
        match.setId(UUID.randomUUID());
        match.setResume(resume);
        match.setJob(job);
        match.setMatchScore(62);
        match.setAnalysisJson(MATCH_ANALYSIS_JSON);
        return match;
    }
}
