# InterviewIQ — AI-Powered Career Assistant

InterviewIQ helps job seekers analyze resumes against ATS criteria, compare
them against target job descriptions, generate tailored cover letters, and
prepare for technical/HR interviews.

This repository is the **Phase 1 scaffold**: project setup, authentication
wiring, database schema, and the external-service integration layer. It
compiles and runs end-to-end (sign in → dashboard confirms the Clerk →
Spring Boot → PostgreSQL chain works), but the feature pages (resume
analysis, job matching, cover letters, interview prep) are intentionally
left as placeholders — those are Phases 2–8 of the build plan below.

---

## 1. Architecture

```
React + Vite (Clerk auth) ──HTTPS/JSON──▶ Spring Boot API ──▶ PostgreSQL (Supabase)
                                                │
                                                ├──▶ Supabase Storage (resume PDFs)
                                                ├──▶ Groq API (resume analysis, matching,
                                                │      cover letters, interview prep)
                                                └──▶ Firecrawl API (job URL extraction)
```

The frontend never talks to Groq, Firecrawl, or Supabase Storage directly —
every external call is proxied and validated by the backend so API keys
never reach the browser.

**Backend package layout** (`backend/src/main/java/com/interviewiq/`):

| Package | Responsibility |
|---|---|
| `config` | Property binding + `WebClient`/security beans for Groq, Firecrawl, Supabase, Clerk, CORS |
| `security` | Clerk JWT verification (`ClerkJwkProvider`, `ClerkJwtValidator`, `ClerkAuthenticationFilter`) and `CurrentUserProvider` |
| `entity` / `repository` | JPA schema (`User`, `Resume`, `ResumeAnalysis`, `Job`, `JobMatch`, `CoverLetter`, `InterviewSession`, `InterviewQuestion`) |
| `service` / `service.impl` | Business logic + external integrations (`GroqService`, `FirecrawlService`, `SupabaseStorageService`, `PdfExtractionService`) |
| `controller` | Thin REST controllers |
| `dto` | Request/response shapes — entities are never returned directly |
| `exception` | Domain exceptions + `GlobalExceptionHandler` (`@RestControllerAdvice`) |

**Frontend layout** (`frontend/src/`): `pages/`, `layouts/`, `routes/`
(auth guard), `services/api.js` (single Axios instance, Clerk token
attached via an interceptor), `hooks/useAuthSync.js`.

---

## 2. Prerequisites

- Java 21+, Maven 3.9+
- Node.js 20+
- A [Supabase](https://supabase.com) project (Postgres + Storage)
- A [Clerk](https://clerk.com) application
- API keys for [Groq](https://console.groq.com) and [Firecrawl](https://firecrawl.dev)

---

## 3. Backend setup

```bash
cd backend
cp .env.example .env   # fill in the values described below
```

| Variable | Where to find it |
|---|---|
| `SUPABASE_DB_HOST/PORT/NAME/USER/PASSWORD` | Supabase → Project Settings → Database → Connection info |
| `SUPABASE_URL` / `SUPABASE_ANON_KEY` / `SUPABASE_SERVICE_ROLE_KEY` | Supabase → Project Settings → API |
| `SUPABASE_STORAGE_BUCKET` | Create a private bucket (e.g. `resumes`) in Supabase → Storage |
| `CLERK_SECRET_KEY` | Clerk Dashboard → API Keys |
| `CLERK_JWT_ISSUER` / `CLERK_JWKS_URL` | Clerk Dashboard → your instance's frontend API URL, e.g. `https://<slug>.clerk.accounts.dev`; JWKS is `<issuer>/.well-known/jwks.json` |
| `GROQ_API_KEY` | Groq Console → API Keys |
| `FIRECRAWL_API_KEY` | Firecrawl Dashboard → API Keys |

`spring-dotenv` loads `.env` automatically in local dev — in production, set
these as real environment variables instead and don't ship a `.env` file.

Create the schema (Phase 1 ships entities with `ddl-auto: validate`, so
either run migrations manually or set `JPA_DDL_AUTO=update` for local
prototyping only):

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. Confirm it's up:

```bash
curl http://localhost:8080/api/health
# {"success":true,"data":{"status":"UP","service":"interviewiq-backend"}}
```

---

## 4. Frontend setup

```bash
cd frontend
cp .env.example .env   # fill in VITE_CLERK_PUBLISHABLE_KEY (Clerk Dashboard -> API Keys)
npm install
npm run dev
```

Open `http://localhost:5173`, sign up/in via Clerk, and you should land on
`/dashboard`, which calls `GET /api/users/me` and reports whether the full
auth chain (Clerk → Spring Security → Postgres) is connected.

---

## 5. API reference (implemented so far)

```http
GET /api/health          # public, no auth
GET /api/users/me        # requires Authorization: Bearer <clerk-session-token>
                          # finds-or-creates the local User row for the caller
```

Example:

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <token from Clerk session>"
```

```json
{
  "success": true,
  "data": {
    "id": "6d1c...",
    "email": "jane@example.com",
    "createdAt": "2026-08-07T10:15:00Z"
  }
}
```

Every other endpoint in the full spec (`/api/resumes`, `/api/jobs`,
`/api/job-matches`, `/api/cover-letters`, `/api/interviews`, ...) is defined
in the project spec and mapped to a phase below — the DTOs, entities, and
repositories they'll sit on top of already exist.

Errors follow one consistent shape from `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-08-07T10:15:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Resume not found",
  "path": "/api/resumes/..."
}
```

---

## 6. Security notes

- The backend **never trusts a user id from the request body**. Every
  request resolves the caller from `CurrentUserProvider`, which reads the
  `AuthenticatedUser` that `ClerkAuthenticationFilter` placed in the
  Spring Security context after verifying the JWT against Clerk's JWKS.
- `/api/**` requires authentication by default (see `SecurityConfig`);
  only `/api/health` and `/api/public/**` are open.
- Resource-ownership checks (verifying `resource.userId == caller.id`
  before returning/mutating a resume, job, analysis, etc.) belong in each
  feature's service layer as those are built out — `UnauthorizedResourceException`
  is already wired into the global error handler for this.
- Supabase's **service role key** is only ever used server-side
  (`SupabaseStorageServiceImpl`); the frontend never sees it.

---

## 7. Remaining build phases

Phase 1 (this scaffold) covers project setup, auth, schema, and the
integration layer. Still to build, per the spec:

| Phase | Scope |
|---|---|
| 2 | Resume upload → Supabase Storage → PDFBox extraction → persistence |
| 3 | Groq resume analysis (ATS score, strengths/weaknesses, prompt builder + JSON validation) |
| 4 | Job description input (paste + Firecrawl URL extraction) |
| 5 | Resume-vs-job matching |
| 6 | Cover letter generation |
| 7 | Interview preparation (technical/HR/resume-based/job-specific questions) |
| 8 | Dashboard stats wired to real data |
| 9 | Ownership checks, validation, AI response validation hardening |
| 10 | Backend + frontend test suites |

---

## 8. Project structure

```
InterviewIQ/
├── backend/     Spring Boot API (Maven)
├── frontend/    React + Vite app
└── documentation/
    └── ARCHITECTURE.md
```
