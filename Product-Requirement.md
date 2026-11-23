# InterviewIQ: Product Requirements

| | |
|---|---|
| **Product** | InterviewIQ, an AI career assistant |
| **Platform** | Responsive web app (desktop down to phone width) |
| **Status** | Core feature set implemented; this document describes the product as built plus open items |
| **Last updated** | 2026-11-22 |
| **Related docs** | [`PRODUCT.md`](PRODUCT.md) (brand and principles), [`Design-System.md`](Design-System.md) (UI system), [`documentation.md`](documentation.md) (technical docs) |

---

## 1. Summary

InterviewIQ helps a job seeker get from "I have a resume" to "I'm ready for the interview" in one place. A user uploads a resume and gets an ATS-style analysis. They save the jobs they are targeting, either pasted or pulled from a URL, and compare the resume against each job. From a match they can generate a tailored cover letter, a set of practice interview questions and a week-by-week learning roadmap for the skills they are missing. Every saved job also moves through an application pipeline (Saved → Applied → Interviewing → Offer), so the user can see where each application stands and what needs follow-up.

## 2. Problem

Job seekers juggle several disconnected tools: an ATS checker, a notes doc or spreadsheet of applications, a generic AI chat for cover letters, and random question lists for interview prep. None of these tools know about each other, so the user re-explains their resume and the job every time, and nothing tells them what to do next. Generic feedback ("add more keywords") isn't actionable, and padded scores give false confidence.

## 3. Goals and non-goals

### Goals
1. **One connected pipeline.** Resume → job → match → (cover letter, interview prep, roadmap) → application tracking, where every step reuses what the user already gave us.
2. **Honest, specific feedback.** Every score comes with the evidence behind it: matched and missing skills, concrete weaknesses, specific suggestions.
3. **A clear next action on every screen.** The user always knows where they are in the pipeline and what to do next.
4. **AI output that is trustworthy in shape.** Structured AI output is validated against a schema and retried; the user never sees malformed or half-parsed results.
5. **Private by default.** A user's resumes, jobs and generated content are visible only to them.

### Non-goals (current scope)
- Applying to jobs on the user's behalf, or any automatic stage change in the tracker. The app may *suggest* a move; only the user makes it.
- A resume builder/editor. We analyse uploaded PDFs; we don't author resumes.
- Job search or job discovery. Users bring their own target jobs.
- Recruiter, employer or team features. This is a single-user product.
- Live/voice mock interviews or answer grading.
- Non-PDF resume formats (DOCX, images).

## 4. Users

A broad mix of job seekers with no single primary persona:

| Segment | Situation | What they need most |
|---|---|---|
| Active searcher | Applying to many roles at once | Tracking, follow-up reminders, fast tailoring per job |
| New grad | Thin experience, unsure what employers want | Gap analysis, a learning roadmap, interview practice |
| Career switcher | Transferable skills, unfamiliar keywords | Match breakdown that shows which skills transfer and which are missing |

Common context: users are mid-task and often under time pressure. They want the next step, not a dashboard to explore.

## 5. Product principles

1. One clear next action per screen; the pipeline position is always visible.
2. Show the number, then the reason.
3. Calm density: enough to decide, never a wall of widgets.
4. Brand lives in precise details (type, color restraint, microcopy), not decoration.
5. AI output is provisional: loading, failure and retry states are first-class.

## 6. Core user journey

```
Sign up / Sign in (Clerk)
        │
        ▼
Upload resume (PDF) ──▶ text extracted ──▶ Analyze ──▶ ATS + overall score, strengths, weaknesses, suggestions
        │
        ▼
Save a target job (paste description OR import from URL)
        │
        ▼
Match resume vs job ──▶ match score, matched/missing skills, keyword gaps, recommendations
        │
        ├──▶ Generate cover letter (tailored, editable, regenerable)
        ├──▶ Generate interview prep (12 questions across 4 categories)
        └──▶ Generate learning roadmap (1–8 weeks, tasks the user ticks off)
        │
        ▼
Track the application (Saved → Applied → Interviewing → Offer / Rejected / Withdrawn)
        │
        ▼
Dashboard: latest score, counts, recent items, pipeline, what needs attention
```

## 7. Functional requirements

Priority key: **P0** must have (shipped), **P1** should have, **P2** nice to have.

### 7.1 Authentication and account (P0)
- FR-1.1 Users sign up and sign in with Clerk (hosted UI at `/login` and `/sign-up`).
- FR-1.2 Every app route except login/sign-up requires a signed-in user; unauthenticated users are redirected to login.
- FR-1.3 On first authenticated request, the backend finds or creates a local user record keyed by the Clerk user id.
- FR-1.4 The backend identifies the caller only from the verified session token, never from a user id in the request.

**Acceptance:** a signed-out visit to `/dashboard` lands on login; a signed-in user sees only their own data; a request with a tampered or expired token gets `401`.

### 7.2 Resumes (P0)
- FR-2.1 Upload a PDF resume (max 10 MB). Non-PDF files and oversize files are rejected with a clear message.
- FR-2.2 The file is stored privately; the extracted text is saved for analysis. Processing status is shown: `UPLOADED`, `PROCESSING`, `COMPLETED` or `FAILED`.
- FR-2.3 List all of the user's resumes, newest first, with status and latest score.
- FR-2.4 Delete a resume. Deletion removes everything that depends on it (analyses, matches, roadmaps, cover letters, interview sessions and questions).

**Acceptance:** uploading a 12 MB file returns "File too large"; uploading a `.docx` returns "Only PDF resumes are supported right now"; a scanned PDF with no extractable text ends as `FAILED` with a readable reason.

### 7.3 Resume analysis (P0)
- FR-3.1 Analyze a resume whose status is `COMPLETED`.
- FR-3.2 Output: ATS score (0–100), overall score (0–100), strengths, weaknesses, missing skills, formatting issues, keyword suggestions and improvement suggestions.
- FR-3.3 Scores are shown with a verdict: **80+ Strong**, **50–79 Needs work**, **under 50 Weak**, always with the number and the word, never color alone.
- FR-3.4 The latest analysis is retrievable without re-running the AI.

### 7.4 Jobs (P0)
- FR-4.1 Save a job by pasting its description (required, up to 50,000 characters), with optional title and company.
- FR-4.2 Save a job from a URL (`http`/`https` only). The posting is scraped server-side and stored as the description.
- FR-4.3 List, view and delete jobs. Deleting a job removes its matches, roadmaps, cover letters, interview sessions and application timeline.

**Acceptance:** `ftp://…` or a malformed URL is rejected before any scrape; a scrape failure shows "Unable to extract the job posting right now" and the user can fall back to pasting.

### 7.5 Resume-vs-job match (P0)
- FR-5.1 Compare one resume against one job.
- FR-5.2 Output: match score (0–100), matching skills, missing skills, matching experience, missing requirements, keyword gaps and recommendations.
- FR-5.3 Show coverage proportionally (e.g. "4 of 6 · 67%") with matched vs missing items listed.
- FR-5.4 From a match, the user can go directly to a cover letter, interview prep or a roadmap for that pair.

### 7.6 Cover letters (P0)
- FR-6.1 Generate a cover letter for a resume + job pair, with an optional candidate name (≤ 200 chars).
- FR-6.2 The letter is 3–4 paragraphs, professional, and ready to send.
- FR-6.3 List, view, copy, regenerate and delete letters.

### 7.7 Interview preparation (P0)
- FR-7.1 Create a session for a resume + job pair and a target role (required, ≤ 200 chars).
- FR-7.2 Generate 12 questions: 3 each in **Technical**, **HR**, **Resume-based** and **Job-specific**, each with a difficulty (Easy/Medium/Hard), a suggested answer and key points.
- FR-7.3 Filter questions by category; regenerate or delete a session.

### 7.8 Learning roadmaps (P0)
- FR-8.1 Generate one roadmap per job match, with an optional weekly time budget (2–40 hours, default 8).
- FR-8.2 A roadmap is 1–8 weeks. Each week has a focus, skills, a milestone and 1–5 tasks. Each task has a title, a description, a type, a suggested resource and a search query.
- FR-8.3 Users tick tasks complete. Completion is user state, stored separately from the AI output. Progress is shown per week and overall.
- FR-8.4 List all roadmaps with progress; view, and delete a roadmap.

### 7.9 Application tracker (P0)
- FR-9.1 Every saved job has a stage: **Saved, Applied, Interviewing, Offer, Rejected, Withdrawn** (the last two are "closed").
- FR-9.2 Board view with one column per stage. Cards move by drag-and-drop or a stage menu.
- FR-9.3 A detail drawer edits stage, applied date, follow-up date and notes (≤ 5,000 chars), and shows the stage-change timeline.
- FR-9.4 Moving to **Applied** fills a missing applied date with today and a missing follow-up date with applied + 7 days.
- FR-9.5 **Needs attention** rules, evaluated in the user's local date:
  - follow-up date is today or past → "Follow up today" / "Follow-up overdue since …";
  - otherwise, an Applied or Interviewing card with no follow-up date and no stage change for 14+ days → "No update in N days".
- FR-9.6 Stage suggestions: when a user action implies progress, the app offers a one-click forward move. Generating a cover letter suggests **Applied**; generating interview prep suggests **Interviewing**. The app never moves a stage on its own, and never suggests a backward move or a move for a closed application.

### 7.10 Dashboard (P0)
- FR-10.1 Shows the latest resume score, counts of resumes, job matches, cover letters and interview sessions, and the 5 most recent resumes and matches.
- FR-10.2 Shows the application pipeline as a proportional bar with per-stage counts, plus items that need attention.
- FR-10.3 A new user with no data sees an empty state pointing at the first step (upload a resume).

## 8. AI behaviour requirements

| Requirement | Detail |
|---|---|
| Structured output | Analysis, match, interview prep and roadmap responses are JSON-only and parsed into typed records |
| Validation | Each response is checked against its schema (score ranges, required lists, week/task counts) |
| Retry | Up to 2 attempts with a corrective retry prompt; then the user sees "The AI service returned an unexpected response. Please try again" |
| Determinism | Structured calls use low temperature (0.2); cover letters use 0.5 for more natural prose |
| Server-side only | The browser never calls the LLM, the scraper or storage directly |
| Honesty | Prompts ask for specific, evidence-based feedback; scores are not inflated |
| Model | Configurable via `GROQ_MODEL`; must be a model the provider still serves |

## 9. Non-functional requirements

### Security and privacy
- All `/api/**` endpoints require authentication except `/api/health` and `/api/public/**`.
- Every read and write checks ownership in the service layer; another user's resource returns `403` or `404`.
- Resume files live in private storage; the service-role key exists only on the backend.
- Error responses never leak stack traces or internal class names.
- Secrets live in environment variables, never in tracked files.

### Reliability and error handling
- Every async panel has a loading skeleton, an empty state and a failure banner with Retry.
- External failures map to clear statuses: AI or storage or scraping failure → `502`; bad PDF → `422`; oversize upload → `413`; validation → `400`.
- Deletes remove dependent rows deepest-first so no orphaned rows remain.

### Performance
- Dashboard is in the main bundle; all other pages are lazy-loaded.
- Skeletons render on first paint with no extra network cost.
- AI generation calls are synchronous today; the user sees a busy state on the triggering button.

### Accessibility
- Target WCAG 2.1 AA: 4.5:1 contrast for small text, visible focus rings, interactive targets ≥ 40px.
- Every field has a label (not placeholder-only). Status is never conveyed by color alone.
- Dialogs and drawers move focus in, trap Tab and restore focus on close.
- Motion respects `prefers-reduced-motion`.

### Compatibility
- Current Chrome, Safari, Firefox and Edge; responsive from 320px wide. The board scrolls horizontally inside its own container; the page never scrolls sideways.

## 10. Limits and validation

| Field | Rule |
|---|---|
| Resume file | PDF only, ≤ 10 MB |
| Job title / company | ≤ 255 chars, optional |
| Job description | Required, ≤ 50,000 chars |
| Job URL | Required for import, `http`/`https`, valid host, ≤ 2,048 chars |
| Cover letter candidate name | ≤ 200 chars, optional |
| Interview role | Required, ≤ 200 chars |
| Roadmap hours per week | 2–40, default 8 |
| Roadmap shape | 1–8 weeks, 1–5 tasks per week |
| Application notes | ≤ 5,000 chars |
| Follow-up default | Applied date + 7 days |
| Stale threshold | 14 days without a stage change |

## 11. Success metrics

| Metric | Definition | Why it matters |
|---|---|---|
| Activation | % of new users who upload a resume **and** get an analysis in their first session | First value moment |
| Pipeline depth | % of active users with ≥ 1 job match | Users are using the connected flow, not just the ATS checker |
| Tailoring rate | Cover letters + interview sessions per job match | The match is driving next steps |
| Tracker adoption | % of saved jobs moved past Saved | The tracker is in use |
| Follow-up hygiene | % of attention items resolved within 3 days | Reminders are useful |
| Roadmap progress | Median % of roadmap tasks completed after 2 weeks | Roadmaps are realistic |
| AI reliability | % of AI requests that succeed within 2 attempts | Users rarely see AI failures |
| Return rate | 7-day and 30-day return | Ongoing value through a job search |

(No analytics are instrumented yet; see open items.)

## 12. Open items and roadmap

### Known accessibility and UX gaps (P1)
- Jobs form field labels; mobile nav drawer focus handling; per-route page titles; a skip link; sidebar tagline contrast.
- Some controls are 36px and should be raised to 40px.
- The segmented control has two visual forms; pick one.

### Next candidates
| Priority | Item |
|---|---|
| P1 | Background processing for long AI calls (queue + polling) instead of blocking requests |
| P1 | Database migrations tool (Flyway/Liquibase) instead of hand-run SQL |
| P1 | Product analytics for the metrics above |
| P1 | Rate limiting / usage caps on AI endpoints |
| P2 | Edit and save cover letter text in-app |
| P2 | DOCX resume support |
| P2 | Answer practice with AI feedback on the user's answers |
| P2 | Email or calendar reminders for follow-ups |
| P2 | Export (cover letter as PDF/DOCX, tracker as CSV) |
| P2 | Account data export and full account deletion |

## 13. Assumptions and dependencies

- **Clerk** for identity; **Supabase** for Postgres and private file storage; **Groq** for the LLM; **Firecrawl** for job-page scraping. An outage in any of these degrades the matching feature with a `502` and a retry path.
- Resumes are text-based PDFs. Image-only scans won't extract.
- Scores are guidance, not a prediction of any specific employer's ATS.
