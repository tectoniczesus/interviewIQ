# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

InterviewIQ: an AI career assistant (resume ATS analysis, resume-vs-job matching, cover letters, interview prep, learning roadmaps, application tracking). Monorepo with `backend/` (Spring Boot 3.3, Java 21, Maven) and `frontend/` (React 18 + Vite + Tailwind, Clerk auth). Resume, analysis, job, job-match, cover-letter, interview, roadmap, application-tracker and dashboard features are all implemented. Docs: `documentation.md` (technical reference), `documentation/ARCHITECTURE.md` (design rationale), `Product-Requirement.md` (requirements), `PRODUCT.md` (users, brand, principles) and `Design-System.md` (the shipped UI system: tokens, components, motion). Read `PRODUCT.md` and `Design-System.md` before UI work. `DESIGN.md` is an earlier visual direction: its principles still apply, but its colors and fonts are superseded by `Design-System.md` and `frontend/src/index.css`.

## Commands

Backend (run from `backend/`). There is no Maven wrapper in the repo, even though the README says `./mvnw`, so use a system `mvn`. If it isn't on PATH, use the copy IntelliJ caches under `~/.m2/wrapper/dists/` (e.g. `~/.m2/wrapper/dists/apache-maven-3.9.16/*/bin/mvn`; several versions are cached). On JDK 25, the Mockito bundled with Boot 3.3 can't mock concrete classes, so mock interfaces only:
```bash
mvn spring-boot:run                      # API on http://localhost:8080
mvn -q compile                           # quick compile check
mvn test                                 # unit tests; opt-in live tests are skipped
mvn test -Dtest=ClassName#methodName     # single test
GROQ_LIVE_TEST=true mvn test -Dtest=LearningRoadmapLiveGroqTest      # real Groq call with the roadmap prompt
RUN_DB_IT=true mvn test -Dtest=LearningRoadmapDbIntegrationTest      # real Supabase DB; writes and removes it_roadmap_* rows
curl localhost:8080/api/health           # smoke check (public endpoint)
```

Frontend (run from `frontend/`):
```bash
npm run dev       # http://localhost:5173
npm run build
npm run lint      # eslint . (no eslint config file is committed yet)
```

## Configuration

- `backend/src/main/resources/application.yml` is **gitignored**. It holds real credentials inlined as `${ENV_VAR:default}` fallbacks. Never commit it, and never copy its values into tracked files. `spring-dotenv` also loads `backend/.env`, and **values in `.env` override the yml defaults**. See `backend/.env.example` for the variable names.
- `GROQ_MODEL` must be a model Groq still serves. `llama-3.3-70b-versatile` has been retired, and it made every AI endpoint return 502. `openai/gpt-oss-120b` works.
- `spring.jpa.open-in-view` is `false`. Lazy associations must be initialized inside the service transaction, using `@EntityGraph` or `Hibernate.initialize`, before controllers map entities to DTOs. Otherwise you get a `LazyInitializationException`.
- `ddl-auto` defaults to `validate` and there is no Flyway/Liquibase. Entity changes need a manual migration on the Supabase project, or `JPA_DDL_AUTO=update` for local prototyping only. Starting the app is the schema check, because it refuses to boot on a mismatch. Hand-written migration SQL lives in `backend/db/migrations/`. When adding a table, follow the existing conventions:
  - a `uuid` primary key
  - `created_at`/`updated_at` as `timestamptz not null`
  - JSON stored in `text` columns, with `columnDefinition = "TEXT"`
  - FKs with no cascade
  - row level security enabled with no policies. The backend connects as the table owner, so it bypasses RLS.
- Frontend env: `VITE_CLERK_PUBLISHABLE_KEY` and `VITE_API_BASE_URL` (default `http://localhost:8080`).

## Architecture

Request flow: React → Spring Boot (`/api/**`) → Postgres (Supabase), plus server-side-only calls to Groq (LLM), Firecrawl (job URL scraping) and Supabase Storage (resume PDFs). The browser never calls these external services directly.

**Auth.** `ClerkAuthenticationFilter` verifies Clerk session JWTs locally against cached JWKS (`ClerkJwkProvider`, `ClerkJwtValidator`) and puts an `AuthenticatedUser` (Clerk id + email) in the SecurityContext. `SecurityConfig` requires auth on all of `/api/**` except `/api/health` and `/api/public/**`. Controllers get the caller from `CurrentUserProvider.getCurrentUser()` and pass the `AuthenticatedUser` into services. Never accept a user id from a request body.

**Ownership checks** live in the service layer, using the `getByIdForUser(id, authenticatedUser)` pattern. It throws `ResourceNotFoundException` or `UnauthorizedResourceException`. Dependent services reuse the owning service's check and don't duplicate it. For example, `ResumeAnalysisServiceImpl` calls `resumeService.getByIdForUser`.

**Response envelopes.** Successful responses are wrapped in `ApiResponse.ok(...)`, which gives `{success, data}`. Errors are never built in controllers: throw a domain exception from `exception/` and `GlobalExceptionHandler` turns it into `ErrorResponse` (`{timestamp, status, error, message, path}`) without leaking internals. Entities are never returned directly. Map them to DTOs in `dto/` through static `from(...)` methods or the classes in `mapper/`.

**AI pipeline.** Each feature follows the same pattern:
1. A prompt builder in `service/prompt/` produces the system prompt, the user prompt and a retry prompt.
2. `GroqService` is a "dumb" HTTP client with no domain knowledge. `complete()` forces JSON-object mode for structured output. `completeText()` returns free-form prose and is used for cover letters.
3. The feature service parses the raw JSON into a record in `dto/ai/`, then calls its `validate()` method. If validation fails it retries up to `MAX_ATTEMPTS`, then throws `InvalidAiResponseException`.
4. The validated result is saved, usually as a JSON column such as `ResumeAnalysis.analysisJson` plus a few extracted scalar fields.

Keep prompt text out of controllers and out of `GroqService`.

**Storage.** Resume PDFs go only to Supabase Storage, through `SupabaseStorageService`, which is the only class that uses the service-role key. Postgres stores just the storage path and the text that PDFBox extracted (`PdfExtractionService`). Analysis requires `Resume.processingStatus == COMPLETED`.

**Deletes.** Postgres has no cascade, so service code deletes dependent rows deepest-first. `ResumeServiceImpl.deleteForUser` deletes interview questions, then sessions, then cover letters, then learning roadmaps, then job matches, then analyses, then the resume itself. `JobServiceImpl.deleteForUser` has the same chain minus analyses, plus `application_events` (the tracker timeline) before the job. `learning_roadmaps.job_match_id` is a required FK, so roadmaps always go before job matches. Update both chains whenever you add an entity with an FK to `Resume` or `Job`, along with `DeleteChainOrderTest`, which asserts the order with Mockito `InOrder`.

**Learning roadmaps.** There is one roadmap per job match (`learning_roadmaps.job_match_id` is unique). It is generated with `POST /api/job-matches/{matchId}/roadmap`. The AI output is stored in `roadmap_json`. Task completion is user state: it is kept separately in `completed_task_ids` and toggled with `PATCH /api/roadmaps/{id}/tasks/{taskId}`.

**Application tracker.** Each `Job` carries its pipeline stage (`ApplicationStatus`), applied and follow-up dates, and notes. `ApplicationEvent` rows record stage changes. The API is `/api/applications/{jobId}`, and the PUT replaces the whole state. Only the user moves stages. Pages call `useStageSuggestion().suggestStage(jobId, target, reason)` to offer a one-click move. Attention and stale rules live in `frontend/src/lib/applications.js` and use the user's local date.

**Frontend.**
- All HTTP calls go through the single Axios instance in `src/services/api.js`, with one feature module per resource (`resumeApi.js`, `jobApi.js`, …).
- `useAuthSync` (called once in `App.jsx`) registers Clerk's `getToken` with the Axios request interceptor, so pages never deal with tokens.
- The response interceptor unwraps the backend error envelope into an `Error` with `.message` and `.status`.
- Routes are guarded by `routes/ProtectedRoute.jsx` inside `layouts/MainLayout.jsx`.
