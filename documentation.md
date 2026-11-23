# InterviewIQ: Technical Documentation

InterviewIQ is an AI career assistant. Its features are resume ATS analysis, resume-vs-job matching, cover letters, interview prep, learning roadmaps and an application tracker. This is the developer reference for the codebase as it stands today.

| Doc | Purpose |
|---|---|
| [`Product-Requirement.md`](Product-Requirement.md) | What the product does and why (requirements, limits, metrics) |
| [`Design-System.md`](Design-System.md) | UI tokens, components, motion, accessibility |
| [`PRODUCT.md`](PRODUCT.md) | Brand, users and design principles (short form) |
| [`DESIGN.md`](DESIGN.md) | Earlier visual direction. Its principles still apply; its colors and fonts are superseded by `Design-System.md` |
| [`CLAUDE.md`](CLAUDE.md) | Working notes and conventions for AI coding agents |
| [`documentation/ARCHITECTURE.md`](documentation/ARCHITECTURE.md) | Design rationale: why auth, ownership, envelopes, the AI pipeline, deletes and the tracker work the way they do |

---

## 1. Tech stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite 5, Tailwind CSS 3, React Router 6, Axios, Clerk React |
| Backend | Spring Boot 3.3 (Web, Validation, Security, Data JPA, WebFlux `WebClient`), Java 21, Maven |
| Database | PostgreSQL on Supabase |
| File storage | Supabase Storage (private bucket for resume PDFs) |
| Auth | Clerk (session JWTs verified locally against JWKS with `jjwt`) |
| LLM | Groq (OpenAI-compatible chat completions); default model `openai/gpt-oss-120b` |
| Scraping | Firecrawl (job posting URL → text) |
| PDF | Apache PDFBox 3 |

---

## 2. Architecture

```
Browser (React + Clerk)
   │  Authorization: Bearer <Clerk session JWT>
   ▼
Spring Boot API  /api/**
   ├── ClerkAuthenticationFilter ── verifies JWT (cached JWKS) ──▶ SecurityContext(AuthenticatedUser)
   ├── Controllers (thin) ──▶ Services (ownership + business rules) ──▶ JPA repositories ──▶ Postgres
   ├── SupabaseStorageService ──▶ Supabase Storage (resume PDFs; only user of the service-role key)
   ├── GroqService ──────────────▶ Groq API (LLM)
   └── FirecrawlService ─────────▶ Firecrawl API (job URL scraping)
```

The browser never calls Groq, Firecrawl or Supabase Storage directly. All keys stay on the server.

### 2.1 Backend packages (`backend/src/main/java/com/interviewiq/`)

| Package | Responsibility |
|---|---|
| `config` | Property binding and beans: Clerk, Groq, Firecrawl, Supabase, upload limits, CORS (`WebConfig`), `SecurityConfig` |
| `security` | `ClerkJwkProvider`, `ClerkJwtValidator`, `ClerkAuthenticationFilter`, `AuthenticatedUser`, `CurrentUserProvider` |
| `controller` | REST endpoints; resolve the caller, call a service, wrap the result in `ApiResponse.ok(...)` |
| `service` / `service.impl` | Business logic, ownership checks, AI orchestration, external clients |
| `service.prompt` | One prompt builder per AI feature (system, user and retry prompts) |
| `entity` / `entity.enums` | JPA entities (all extend `BaseEntity`: `id`, `createdAt`, `updatedAt`) |
| `repository` | Spring Data JPA repositories |
| `dto` | Request/response records with static `from(...)` mappers; `dto.ai` holds typed AI outputs with `validate()` |
| `mapper` | Mappers for entities that carry parsed JSON (`JobMatchMapper`, `ResumeAnalysisMapper`, `LearningRoadmapMapper`) |
| `exception` | Domain exceptions, `ErrorResponse`, `GlobalExceptionHandler` |

### 2.2 Frontend layout (`frontend/src/`)

| Path | Responsibility |
|---|---|
| `main.jsx` | React root, `ClerkProvider`, router |
| `App.jsx` | Routes. Dashboard is eagerly loaded; other pages are `lazy()` |
| `hooks/useAuthSync.js` | Registers Clerk's `getToken` with the Axios request interceptor (called once in `App`) |
| `routes/ProtectedRoute.jsx` | Redirects signed-out users to `/login` |
| `layouts/MainLayout.jsx` | Sidebar / mobile drawer shell and nav |
| `services/api.js` | The single Axios instance. The response interceptor unwraps the error envelope into `Error{message,status}` |
| `services/*Api.js` | One module per resource: `resumeApi`, `jobApi`, `jobMatchApi`, `coverLetterApi`, `interviewApi`, `roadmapApi`, `applicationApi`, `dashboardApi` |
| `pages/` | One component per route |
| `components/` | Shared UI: `PageHeader`, `ScoreCard`, `StagePill`, `StatusBadge`, `StageSuggestion`, `MatchBreakdown`, `Skeleton`, `roadmap`, `icons` |
| `lib/applications.js` | Tracker rules: stages, attention/stale logic, `buildUpdate`, local-date helpers |
| `lib/time.js` | Relative time formatting |
| `index.css` / `tailwind.config.js` | Design tokens and component classes (see `Design-System.md`) |

### 2.3 Frontend routes

| Route | Page |
|---|---|
| `/login/*`, `/sign-up/*` | Clerk sign-in / sign-up (public) |
| `/dashboard` | Dashboard (default, and the fallback for unknown routes) |
| `/resumes` | Upload and list resumes |
| `/resumes/:id/analysis` | Resume analysis |
| `/jobs` | Save jobs (paste or URL) and list them |
| `/jobs/:id/match` | Match a resume against a job |
| `/applications` | Application tracker board |
| `/roadmaps`, `/roadmaps/:id` | Learning roadmap list and detail |
| `/cover-letters` | Generate and manage cover letters |
| `/interview-prep` | Generate and review interview questions |

---

## 3. Local setup

### 3.1 Prerequisites
- Java 21+ and Maven 3.9+. There is **no Maven wrapper** in the repo, so use a system `mvn`. If it isn't on PATH, the copy IntelliJ caches works: `~/.m2/wrapper/dists/apache-maven-3.9.*/*/bin/mvn`.
- Node.js 20+.
- A Supabase project (Postgres + a private Storage bucket), a Clerk application, and Groq and Firecrawl API keys.

### 3.2 Backend

```bash
cd backend
cp .env.example .env        # fill in values (see 3.4)
mvn spring-boot:run         # http://localhost:8080
curl localhost:8080/api/health
# {"success":true,"data":{"status":"UP","service":"interviewiq-backend"}}
```

### 3.3 Frontend

```bash
cd frontend
cp .env.example .env        # VITE_CLERK_PUBLISHABLE_KEY, VITE_API_BASE_URL
npm install
npm run dev                 # http://localhost:5173
```

### 3.4 Configuration

`backend/src/main/resources/application.yml` is **gitignored**. It reads every value as `${ENV_VAR:default}`. `spring-dotenv` loads `backend/.env`, and values in `.env` **override** the yml defaults. Never commit real credentials.

| Variable | Purpose |
|---|---|
| `SERVER_PORT` | API port (default 8080) |
| `FRONTEND_ORIGIN` | Allowed CORS origin (default `http://localhost:5173`) |
| `JPA_DDL_AUTO` | `validate` (default). Use `update` only for local prototyping |
| `SUPABASE_DB_HOST`, `_PORT`, `_NAME`, `_USER`, `_PASSWORD` | Postgres connection (Supabase → Project Settings → Database) |
| `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY` | Supabase API (Project Settings → API). The service-role key is server-only |
| `SUPABASE_STORAGE_BUCKET` | Private bucket for resume PDFs (e.g. `resumes`) |
| `CLERK_SECRET_KEY` | Clerk Dashboard → API Keys |
| `CLERK_JWT_ISSUER` | Your Clerk frontend API URL, e.g. `https://<slug>.clerk.accounts.dev` |
| `CLERK_JWKS_URL` | `<issuer>/.well-known/jwks.json` |
| `GROQ_API_KEY` | Groq Console → API Keys |
| `GROQ_MODEL` | Must be a model Groq still serves. `llama-3.3-70b-versatile` is retired and makes every AI endpoint return 502. `openai/gpt-oss-120b` works |
| `GROQ_BASE_URL` | Defaults to `https://api.groq.com/openai/v1` |
| `FIRECRAWL_API_KEY` | Firecrawl Dashboard → API Keys |

Frontend (`frontend/.env`):

| Variable | Purpose |
|---|---|
| `VITE_CLERK_PUBLISHABLE_KEY` | Clerk publishable key |
| `VITE_API_BASE_URL` | Backend base URL (default `http://localhost:8080`) |

Upload limits are in `application.yml`: 10 MB max file and request size, allowed type `application/pdf`.

---

## 4. Commands

### Backend (from `backend/`)

```bash
mvn spring-boot:run                     # run the API
mvn -q compile                          # quick compile check
mvn test                                # unit tests (opt-in live tests are skipped)
mvn test -Dtest=ClassName#methodName    # a single test
GROQ_LIVE_TEST=true mvn test -Dtest=LearningRoadmapLiveGroqTest   # real Groq call
RUN_DB_IT=true mvn test -Dtest=LearningRoadmapDbIntegrationTest   # real Supabase DB; writes and removes it_roadmap_* rows
```

### Frontend (from `frontend/`)

```bash
npm run dev       # dev server
npm run build     # production build to dist/
npm run preview   # serve the build
npm run lint      # eslint . (no eslint config is committed yet)
```

---

## 5. Authentication and authorization

1. The frontend gets a session token from Clerk. `useAuthSync` hands `getToken` to the Axios interceptor, which sets `Authorization: Bearer <jwt>` on every request.
2. `ClerkAuthenticationFilter` verifies the token's signature against Clerk's JWKS. `ClerkJwkProvider` caches the keys by `kid` for an hour. The filter also checks the issuer and expiry, then puts an `AuthenticatedUser(clerkUserId, email)` in the `SecurityContext`.
3. `SecurityConfig` requires authentication on all of `/api/**` except `/api/health` and `/api/public/**`.
4. Controllers call `CurrentUserProvider.getCurrentUser()` and pass the `AuthenticatedUser` to services. **A user id is never accepted from a request body or path.**
5. **Ownership** is enforced in services with `getByIdForUser(id, user)`. It throws `ResourceNotFoundException` (404) or `UnauthorizedResourceException` (403). Dependent services reuse the owner's check: for example, `ResumeAnalysisServiceImpl` calls `resumeService.getByIdForUser`.
6. `GET /api/users/me` finds or creates the local `users` row for the caller.

---

## 6. API reference

Every endpoint requires a Bearer token unless marked public. Success responses use this envelope:

```json
{ "success": true, "data": { ... } }
```

Errors come only from `GlobalExceptionHandler`:

```json
{ "timestamp": "2026-11-22T10:15:00Z", "status": 404, "error": "Not Found",
  "message": "Resume not found", "path": "/api/resumes/..." }
```

### Health and user
| Method | Path | Notes |
|---|---|---|
| GET | `/api/health` | Public smoke check |
| GET | `/api/users/me` | Find-or-create the caller's user |

### Resumes
| Method | Path | Body / params | Notes |
|---|---|---|---|
| POST | `/api/resumes` | `multipart/form-data`, field `file` (PDF ≤ 10 MB) | `201`. Stores the PDF, extracts text, sets `processingStatus` |
| GET | `/api/resumes` | | List the caller's resumes |
| GET | `/api/resumes/{id}` | | Detail |
| DELETE | `/api/resumes/{id}` | | Deletes the dependent chain (see §8) |
| POST | `/api/resumes/{id}/analyze` | | `201`. Runs the AI analysis; requires `COMPLETED` |
| GET | `/api/resumes/{id}/analysis` | | Latest analysis |

### Jobs
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/api/jobs` | `{title?, company?, description}` | `201`. Source `PASTED`. Description required, ≤ 50,000 chars |
| POST | `/api/jobs/from-url` | `{title?, company?, jobUrl}` | `201`. http/https only; scraped with Firecrawl. Source `URL` |
| GET | `/api/jobs` | | List |
| GET | `/api/jobs/{id}` | | Detail |
| DELETE | `/api/jobs/{id}` | | Deletes the dependent chain (see §8) |

### Job matches
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/api/job-matches` | `{resumeId, jobId}` | `201`. AI match |
| GET | `/api/job-matches` | | List |
| GET | `/api/job-matches/{id}` | | Detail with parsed breakdown |

### Learning roadmaps
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/api/job-matches/{matchId}/roadmap` | `{hoursPerWeek?}` (2–40, default 8) | `201`. One roadmap per match |
| GET | `/api/job-matches/{matchId}/roadmap` | | The match's roadmap |
| GET | `/api/roadmaps` | | List with progress |
| GET | `/api/roadmaps/{id}` | | Detail |
| PATCH | `/api/roadmaps/{id}/tasks/{taskId}` | `{completed: boolean}` | Toggle a task; stored in `completed_task_ids` |
| DELETE | `/api/roadmaps/{id}` | | |

### Cover letters
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/api/cover-letters` | `{resumeId, jobId, candidateName?}` | `201`. Free-text generation |
| GET | `/api/cover-letters` | | List |
| GET | `/api/cover-letters/{id}` | | Detail |
| POST | `/api/cover-letters/{id}/regenerate` | | New text for the same pair |
| DELETE | `/api/cover-letters/{id}` | | |

### Interview prep
| Method | Path | Body / params | Notes |
|---|---|---|---|
| POST | `/api/interviews` | `{resumeId, jobId, role}` | `201`. Generates 12 questions |
| GET | `/api/interviews` | | List sessions |
| GET | `/api/interviews/{id}` | | Session |
| GET | `/api/interviews/{id}/questions` | `?category=TECHNICAL\|HR\|RESUME_BASED\|JOB_SPECIFIC` | Optional filter |
| POST | `/api/interviews/{id}/regenerate` | | Replace the questions |
| DELETE | `/api/interviews/{id}` | | Deletes questions, then the session |

### Application tracker
| Method | Path | Body | Notes |
|---|---|---|---|
| GET | `/api/applications` | | Every job with its tracker state |
| GET | `/api/applications/{jobId}` | | One application |
| PUT | `/api/applications/{jobId}` | `{status, appliedOn?, followUpOn?, notes?}` | **Full replacement**: a null date means cleared. Records an `application_events` row on stage change |
| GET | `/api/applications/{jobId}/events` | | Stage-change timeline |

### Dashboard
| Method | Path | Notes |
|---|---|---|
| GET | `/api/dashboard` | `resumeScore`, counts (resumes, matches, cover letters, interview sessions), the 5 most recent resumes and matches |

### Error mapping

| Exception | Status | `error` |
|---|---|---|
| `ValidationException`, bean validation, bad type, missing part, malformed body | 400 | Invalid Request |
| `InvalidFileException` | 400 | Invalid File |
| `NotAuthenticatedException` | 401 | Unauthorized |
| `UnauthorizedResourceException` | 403 | Forbidden |
| `ResourceNotFoundException` | 404 | Not Found |
| `MaxUploadSizeExceededException` | 413 | File Too Large |
| `PdfExtractionException` | 422 | PDF Processing Failed |
| `GroqApiException` | 502 | AI Service Unavailable |
| `InvalidAiResponseException` | 502 | AI Response Invalid |
| `FirecrawlApiException` | 502 | Job Extraction Failed |
| `StorageException` | 502 | Storage Error |
| Anything else | 500 | Internal Server Error (no internals leaked) |

---

## 7. Data model

All tables use a `uuid` primary key and `created_at`/`updated_at` columns (`timestamptz not null`). JSON is stored in `text` columns. Foreign keys have **no cascade**, and RLS is enabled with no policies: the backend connects as the table owner.

```
users ─┬─< resumes ─┬─< resume_analyses
       │            ├─< job_matches >─┬─ jobs ─┬─< application_events
       │            │                 │        │
       │            │   learning_roadmaps (1:1 job_match, unique)
       │            ├─< cover_letters >──────── jobs
       │            └─< interview_sessions >─── jobs
       │                     └─< interview_questions
       └─< jobs
```

| Table | Key columns |
|---|---|
| `users` | `clerk_user_id` (unique), `email` |
| `resumes` | `user_id`, `file_name`, `storage_path`, `extracted_text`, `processing_status` (`UPLOADED`/`PROCESSING`/`COMPLETED`/`FAILED`) |
| `resume_analyses` | `resume_id`, `ats_score`, `overall_score`, `analysis_json` |
| `jobs` | `user_id`, `title`, `company`, `job_url`, `description`, `source` (`PASTED`/`URL`), `application_status`, `applied_on`, `follow_up_on`, `application_notes`, `status_changed_at` |
| `job_matches` | `resume_id`, `job_id`, `match_score`, `analysis_json` |
| `learning_roadmaps` | `job_match_id` (unique, required), `target_role`, `roadmap_json`, `completed_task_ids` (JSON array, default `[]`), `total_tasks` |
| `cover_letters` | `user_id`, `resume_id`, `job_id`, `content` |
| `interview_sessions` | `user_id`, `resume_id`, `job_id`, `role` |
| `interview_questions` | `session_id`, `category`, `difficulty`, `question`, `suggested_answer`, `key_points` |
| `application_events` | `job_id`, `from_status` (nullable), `to_status` |

**Enums:** `ApplicationStatus` = `SAVED, APPLIED, INTERVIEWING, OFFER, REJECTED, WITHDRAWN` (the last two are closed). `QuestionCategory` = `TECHNICAL, HR, RESUME_BASED, JOB_SPECIFIC`. `QuestionDifficulty` = `EASY, MEDIUM, HARD`.

### 7.1 Schema changes and migrations

There is **no Flyway/Liquibase**. `ddl-auto` defaults to `validate`, so the app refuses to boot if the entities and the database disagree. Starting the app is the schema check.

1. Change the entity.
2. Write SQL in `backend/db/migrations/YYYY-MM-DD_<name>.sql` following the conventions above. See `2026-11-12_application_tracker.sql` for an example.
3. Run it manually on the Supabase project (SQL editor).
4. Start the app to confirm it validates.

`JPA_DDL_AUTO=update` is acceptable for throwaway local prototyping only.

### 7.2 Lazy loading

`spring.jpa.open-in-view` is `false`. Initialize lazy associations inside the service transaction, with `@EntityGraph` or `Hibernate.initialize`, before a controller maps the entity to a DTO. Otherwise you get a `LazyInitializationException`.

---

## 8. Deletes

Postgres has no cascades, so services delete dependents deepest-first:

- **`ResumeServiceImpl.deleteForUser`:** interview questions → interview sessions → cover letters → learning roadmaps → job matches → analyses → resume (and the stored PDF).
- **`JobServiceImpl.deleteForUser`:** interview questions → interview sessions → cover letters → learning roadmaps → job matches → application events → job.

`learning_roadmaps.job_match_id` is a required FK, so roadmaps always go before job matches. **When you add an entity with an FK to `Resume` or `Job`,** update both chains and `DeleteChainOrderTest`, which asserts the order with Mockito `InOrder`.

---

## 9. AI pipeline

Each AI feature follows the same four steps:

1. **Prompt builder** (`service/prompt/*PromptBuilder`) produces the system prompt, the user prompt and a retry prompt. Prompt text never lives in controllers or in `GroqService`.
2. **`GroqService`** is a plain HTTP client with no domain knowledge.
   - `complete()` forces `response_format: json_object` at temperature 0.2, for structured output.
   - `completeText()` returns prose at temperature 0.5, for cover letters.
3. **Parse and validate.** The feature service parses the JSON into a record in `dto/ai/` and calls `validate()`. If validation fails it retries with the retry prompt, up to `MAX_ATTEMPTS = 2`, then throws `InvalidAiResponseException` (502).
4. **Persist.** The validated result is saved as a JSON column plus a few extracted scalars, such as `ResumeAnalysis.analysisJson` with `atsScore` and `overallScore`.

| Feature | Output record | Shape |
|---|---|---|
| Resume analysis | `ResumeAnalysisAiResponse` | `atsScore`, `overallScore` (0–100), `strengths`, `weaknesses`, `missingSkills`, `formattingIssues`, `keywordSuggestions`, `improvementSuggestions` |
| Job match | `JobMatchAiResponse` | `matchScore` (0–100), `matchingSkills`, `missingSkills`, `matchingExperience`, `missingRequirements`, `keywordGaps`, `recommendations` |
| Interview prep | `InterviewPrepAiResponse` | `questions[]` of `{question, category, difficulty, suggestedAnswer, keyPoints[]}`, 3 per category (12 total) |
| Learning roadmap | `LearningRoadmapAiResponse` | `summary`, `weeks[1..8]` of `{focus, skills[], milestone, tasks[1..5]}`; each task is `{title, description, type, resourceName, searchQuery}` |
| Cover letter | plain text | 3–4 paragraphs, ready to send |

**Adding an AI feature:** write a prompt builder, write a `dto/ai` record with `validate()`, then write a service that uses the retry loop and persists the result. Keep the ownership check through the owning service.

---

## 10. Feature notes

### Resumes and storage
- `ResumeFileValidator` rejects empty files, oversize files and non-PDFs. A file passes if either its content type or its `.pdf` extension matches.
- PDFs go only to Supabase Storage through `SupabaseStorageService`, the only class that uses the service-role key. Postgres stores the storage path and the text that PDFBox extracted (`PdfExtractionService`).
- Analysis requires `processingStatus == COMPLETED`.

### Jobs from URL
`JobUrlValidator` accepts only `http`/`https` URLs with a host. `FirecrawlService` scrapes the page server-side, and the result becomes the job description.

### Learning roadmaps
- One roadmap per job match. The AI output lives in `roadmap_json`.
- Task completion is user state, kept separately in `completed_task_ids` and toggled with `PATCH`.

### Application tracker
- Each `Job` carries its stage, applied and follow-up dates, notes and `status_changed_at`. Every stage change writes an `ApplicationEvent`.
- **Only the user moves stages.** Pages call `useStageSuggestion().suggestStage(jobId, target, reason)` to offer a one-click forward move. Cover letters suggest `APPLIED`; interview prep suggests `INTERVIEWING`.
- The rules in `frontend/src/lib/applications.js` use the **user's local date**:
  - Moving to Applied fills `appliedOn = today` and `followUpOn = appliedOn + 7` if they're missing. The server applies the same fallback.
  - **Follow-up due:** `followUpOn <= today` gives "Follow up today" or "Follow-up overdue since …".
  - **Stale:** an Applied or Interviewing card with no follow-up date and 14+ days since the last stage change gives "No update in N days".

### Dashboard
`DashboardServiceImpl` aggregates counts and the 5 most recent resumes and matches. The frontend adds the pipeline bar and the attention list from `/api/applications`.

---

## 11. Frontend conventions

- **HTTP:** use the Axios instance in `services/api.js` and a per-resource module. Pages never touch tokens. Errors arrive as `Error` objects with `.message` and `.status`, ready to show in a banner.
- **Styling:** use the tokens and component classes from `Design-System.md`. Don't use Tailwind's default color scale or raw hexes.
- **States:** every data panel has a skeleton, an empty state and an error banner with Retry.
- **Code splitting:** new pages should be `lazy()` in `App.jsx`, unless they're the landing route.

---

## 12. Testing

| Test | What it covers |
|---|---|
| `LearningRoadmapAiResponseTest` | AI roadmap schema validation |
| `LearningRoadmapMapperTest` | Roadmap JSON → DTO mapping and progress |
| `LearningRoadmapServiceImplTest` | Generation, retry and task toggling |
| `ApplicationServiceImplTest` | Tracker updates, date fallbacks, events |
| `DeleteChainOrderTest` | Resume and job delete order (`InOrder`) |
| `LearningRoadmapLiveGroqTest` | Opt-in (`GROQ_LIVE_TEST=true`): real Groq call |
| `LearningRoadmapDbIntegrationTest` | Opt-in (`RUN_DB_IT=true`): real Supabase DB |

On JDK 25, the Mockito bundled with Boot 3.3 can't mock concrete classes, so **mock interfaces only**. There are no frontend tests yet.

---

## 13. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| Every AI endpoint returns 502 "AI Service Unavailable" | `GROQ_MODEL` is retired, or the key is invalid. Set `GROQ_MODEL=openai/gpt-oss-120b` |
| 502 "AI Response Invalid" | The model returned JSON that failed `validate()` twice. Retry; check the prompt builder if it persists |
| App won't boot: schema validation error | An entity changed without a migration. Apply the SQL in `backend/db/migrations/` |
| `LazyInitializationException` | A lazy association was read outside the transaction. Initialize it in the service |
| 401 on every request | Clerk issuer/JWKS mismatch, or the frontend isn't sending a token (check `useAuthSync`) |
| CORS error in the browser | `FRONTEND_ORIGIN` doesn't match the dev server origin |
| Upload fails with 413 | File over 10 MB |
| Resume stuck in `FAILED` | Image-only or encrypted PDF; PDFBox found no text |
| A config value isn't what you set | `.env` overrides the `${VAR:default}` fallbacks in `application.yml`; check both files |
| `mvn: command not found` | Use the IntelliJ-cached Maven under `~/.m2/wrapper/dists/` |

---

## 14. Known gaps

- No migration tool (Flyway/Liquibase); migrations are hand-run SQL.
- AI calls are synchronous; there is no job queue for long generations.
- No frontend tests and no committed ESLint config.
- No rate limiting on AI endpoints.- Accessibility open items: Jobs form labels, mobile nav focus handling, per-route page titles, a skip link, sidebar tagline contrast, and a few 36px controls.
