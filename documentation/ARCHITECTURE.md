# Architecture Notes

This file explains **why** the system is built the way it is. For the **what** (setup, endpoints, tables, commands), see [`../documentation.md`](../documentation.md).

```
React + Vite (Clerk) ──▶ Spring Boot /api/** ──▶ Postgres (Supabase)
                               ├──▶ Supabase Storage (resume PDFs)
                               ├──▶ Groq (LLM)
                               └──▶ Firecrawl (job URL scraping)
```

---

## Auth: Clerk JWT verification, locally

The backend doesn't call Clerk on every request. `ClerkJwkProvider` fetches Clerk's JWKS once and caches the public keys by `kid` for an hour. `ClerkJwtValidator` then verifies each session token's signature, issuer and expiry locally.

`ClerkAuthenticationFilter` runs before Spring Security's standard auth filter. It puts an `AuthenticatedUser` record in the `SecurityContext`, holding only what the rest of the app needs: the Clerk user id and email. Every controller resolves the caller through `CurrentUserProvider` and passes that record into the service.

**A user id is never accepted from a request body or path.** The first authenticated call to `GET /api/users/me` finds or creates the local `users` row.

## Ownership lives in the service layer

Every resource is reached through a `getByIdForUser(id, authenticatedUser)` method on its owning service. It throws `ResourceNotFoundException` (404) or `UnauthorizedResourceException` (403).

Dependent features reuse the owner's check rather than writing their own. `ResumeAnalysisServiceImpl` calls `resumeService.getByIdForUser`, and a roadmap is reached through its job match. This keeps one authoritative check per resource type, so a new endpoint can't quietly skip it by querying a repository directly.

## Two response envelopes, on purpose

- **Success:** `{"success": true, "data": {...}}`, implemented as `dto.ApiResponse<T>` and returned by controllers.
- **Error:** `{timestamp, status, error, message, path}`, implemented as `exception.ErrorResponse` and returned **only** by `GlobalExceptionHandler`.

Controllers only ever handle the success case. Every failure, from whichever layer, goes through one place that maps domain exceptions to status codes. For example, AI, scraping and storage failures become `502`, an unreadable PDF becomes `422`, and an oversize upload becomes `413`. That one place also guarantees no stack trace, exception class name or internal detail reaches the client.

Entities are never serialized. They're mapped to DTOs with static `from(...)` methods, or with the classes in `mapper/` when parsed JSON is involved.

## External APIs behind "dumb" services

`GroqService`, `FirecrawlService` and `SupabaseStorageService` know how to make an HTTP call and nothing about resumes or jobs.

`SupabaseStorageService` is the only component that uses the Supabase service-role key. Resume PDFs live in a private bucket. Postgres stores only the storage path and the text extracted by PDFBox. The analysis itself always runs on the stored text, never on the file.

## The AI pipeline: structured, validated, retried

LLM output is treated as untrusted input. Each AI feature has the same four parts:

1. **A prompt builder** in `service/prompt/` produces the system prompt, the user prompt and a retry prompt. Prompt text never lives in controllers or in `GroqService`.
2. **`GroqService.complete()`** forces JSON-object mode at a low temperature (0.2). Cover letters, the one free-text feature, use `completeText()` at 0.5.
3. **A typed record** in `dto/ai/` parses the JSON and has a `validate()` method. The checks include score ranges, required lists, and week and task counts.
4. **The feature service** loops up to `MAX_ATTEMPTS` (2). On a parse or validation failure, it sends the retry prompt, which tells the model what was wrong. After the last failed attempt it throws `InvalidAiResponseException`.

The validated result is stored as a JSON `text` column plus a few extracted scalars for listing and sorting (`atsScore`, `matchScore`, `totalTasks`). That way the "never show malformed AI output" rule is enforced in exactly one place per feature, and adding a field to an AI response doesn't need a schema migration.

## Deletes without cascades

Foreign keys deliberately have **no `ON DELETE CASCADE`**. Instead, `ResumeServiceImpl.deleteForUser` and `JobServiceImpl.deleteForUser` delete dependents explicitly, deepest-first. A resume deletion goes from interview questions up to the resume itself and its stored PDF. A job deletion also removes `application_events`.

Explicit deletes make the blast radius visible in code and reviewable in a diff. They also let the resume deletion remove the file in Storage, which a database cascade couldn't do. The cost is that every new entity with an FK to `Resume` or `Job` must be added to both chains. `DeleteChainOrderTest` asserts the order with Mockito `InOrder` so a missed step fails the build.

## Persistence choices

- **`open-in-view: false`.** Lazy associations are initialized inside the service transaction, with `@EntityGraph` or `Hibernate.initialize`. This keeps queries in the service layer, where they can be seen, and stops controllers from triggering N+1 loads while mapping DTOs.
- **`ddl-auto: validate` with hand-written SQL** in `backend/db/migrations/`. Nobody can point the app at the real Supabase database with `update` by accident, and booting the app doubles as a schema check.
- **Conventions:** `uuid` keys, `created_at`/`updated_at` as `timestamptz`, JSON in `text` columns, and RLS enabled with no policies. The backend connects as the table owner and is the only client, so it bypasses RLS. With no policies, RLS denies all access through Supabase's public API by default.

## Feature-specific decisions

### Learning roadmaps: AI content and user state kept apart
There is one roadmap per job match (`job_match_id` is unique). The AI output lives in `roadmap_json`, and task completion lives separately in `completed_task_ids`. Ticking a task is a cheap `PATCH` that never rewrites the AI content, and progress can't be lost by re-parsing it.

### Application tracker: the user is the only one who moves a stage
The pipeline stage lives on `Job` itself rather than a separate application table, because every saved job is a potential application. `ApplicationEvent` rows record each change for the timeline.

The app never changes a stage automatically. When an action implies progress, such as generating a cover letter or interview prep, the page offers a one-click suggestion through `useStageSuggestion()`. An automatic move would make the board untrustworthy the first time it guessed wrong.

`PUT /api/applications/{jobId}` replaces the whole state, so a `null` date means "cleared", not "unchanged". This avoids a class of partial-update ambiguity.

**Dates are the user's local dates.** The "follow up today", "overdue" and "stale after 14 days" rules live in `frontend/src/lib/applications.js` and use the viewer's time zone. A server in UTC would otherwise flag follow-ups a day early or late. Moving to Applied fills a missing applied date (today) and follow-up date (+7 days) on the client, and the server applies the same fallback.

## Frontend structure

- **One Axios instance** (`services/api.js`) with one module per resource. `useAuthSync` registers Clerk's `getToken` with the request interceptor once, so pages never handle tokens. The response interceptor turns the error envelope into an `Error` with `.message` and `.status`, ready for a banner.
- **Route-level code splitting.** The Dashboard (the landing route) ships in the main bundle and every other page is `lazy()`, which keeps the first paint and Clerk's login screen light.
- **Design tokens as CSS variables** (`index.css`), mapped into Tailwind (`tailwind.config.js`), so opacity modifiers work and a theme change is one file. See [`../Design-System.md`](../Design-System.md).

## Known gaps

- **No migration tool.** Hand-run SQL works at this size. Add Flyway or Liquibase before a second environment or a second developer makes drift likely.
- **AI calls are synchronous.** Requests block while Groq generates. The service boundaries above are what make a later move to a queue plus polling possible without a redesign: controllers would return a job id, and the feature service would run the same pipeline in a worker.
- **No rate limiting** on AI endpoints.
- **Thin test coverage.** There are backend unit tests for roadmaps, the tracker and the delete order, plus opt-in live Groq and database tests, and no frontend tests. On JDK 25, mock interfaces only, because Boot 3.3's Mockito can't mock concrete classes.
