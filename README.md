# InterviewIQ: AI-Powered Career Assistant

InterviewIQ takes a job seeker from "I have a resume" to "I'm ready for the interview" in one place. You upload a resume, compare it against the jobs you're targeting, generate the material you need for each job, and track every application through a pipeline.

## Features

| Feature | What it does |
|---|---|
| **Resume analysis** | Upload a PDF to get an ATS score and an overall score (0–100), with strengths, weaknesses, missing skills, formatting issues and keyword suggestions |
| **Jobs** | Save target jobs by pasting the description or importing it from a URL |
| **Job match** | Compare a resume against a job: match score, matched and missing skills, keyword gaps and recommendations |
| **Cover letters** | Generate a tailored 3–4 paragraph letter for a resume + job pair, and regenerate it if needed |
| **Interview prep** | 12 practice questions (technical, HR, resume-based, job-specific), each with a suggested answer and key points |
| **Learning roadmaps** | A 1–8 week plan that closes the skill gaps from a match, with tasks you tick off |
| **Application tracker** | A board (Saved → Applied → Interviewing → Offer / Rejected / Withdrawn) with follow-up dates, notes, a timeline and "needs attention" flags |
| **Dashboard** | Latest score, counts, recent activity, the application pipeline and what needs follow-up |

## Architecture

```
React + Vite (Clerk auth) ──HTTPS/JSON──▶ Spring Boot API ──▶ PostgreSQL (Supabase)
                                                │
                                                ├──▶ Supabase Storage (resume PDFs)
                                                ├──▶ Groq API (analysis, matching, letters,
                                                │      interview prep, roadmaps)
                                                └──▶ Firecrawl API (job URL extraction)
```

The browser only talks to the Spring Boot API. Every call to Groq, Firecrawl or Supabase Storage happens on the server, so API keys never reach the browser.

| Layer | Stack |
|---|---|
| Frontend | React 18, Vite 5, Tailwind CSS 3, React Router 6, Axios, Clerk |
| Backend | Spring Boot 3.3, Java 21, Maven, Spring Security, Spring Data JPA, PDFBox |
| Data | PostgreSQL and Storage on Supabase |
| AI and scraping | Groq (default model `openai/gpt-oss-120b`), Firecrawl |

## Quick start

### Prerequisites
- Java 21+, Maven 3.9+ (there is no Maven wrapper; use a system `mvn`)
- Node.js 20+
- A [Supabase](https://supabase.com) project with a private Storage bucket, a [Clerk](https://clerk.com) application, and API keys for [Groq](https://console.groq.com) and [Firecrawl](https://firecrawl.dev)

### Backend

```bash
cd backend
cp .env.example .env      # fill in the values
mvn spring-boot:run       # http://localhost:8080
curl localhost:8080/api/health
# {"success":true,"data":{"status":"UP","service":"interviewiq-backend"}}
```

`application.yml` is gitignored and reads every setting from environment variables. Values in `backend/.env` override its defaults. See [`backend/.env.example`](backend/.env.example) for the variable names and [`documentation.md`](documentation.md#34-configuration) for what each one is.

The schema is validated on startup (`ddl-auto: validate`). Apply the SQL in [`backend/db/migrations/`](backend/db/migrations/) to your Supabase project before the first run.

### Frontend

```bash
cd frontend
cp .env.example .env      # VITE_CLERK_PUBLISHABLE_KEY, VITE_API_BASE_URL
npm install
npm run dev               # http://localhost:5173
```

Sign up through Clerk and you land on the dashboard.

## Common commands

```bash
# backend/
mvn spring-boot:run                     # run the API
mvn test                                # unit tests
mvn test -Dtest=ClassName#methodName    # a single test

# frontend/
npm run dev                             # dev server
npm run build                           # production build
npm run lint                            # lint
```

## Security

- Every `/api/**` endpoint requires a Clerk session token, except `/api/health` and `/api/public/**`. Tokens are verified locally against Clerk's JWKS.
- The backend never trusts a user id from the request. The caller always comes from the verified token.
- Ownership is checked in the service layer for every resource. Another user's data returns 403 or 404.
- Resume PDFs are stored in a private bucket. The Supabase service-role key is used only on the server.
- Errors use one envelope (`{timestamp, status, error, message, path}`) and never expose internals.

## Documentation

| Doc | What's in it |
|---|---|
| [`documentation.md`](documentation.md) | Technical reference: setup, configuration, full API, data model, AI pipeline, testing, troubleshooting |
| [`Product-Requirement.md`](Product-Requirement.md) | Product requirements: goals, users, feature requirements, limits, metrics, roadmap |
| [`Design-System.md`](Design-System.md) | UI tokens, components, motion and accessibility, as shipped |
| [`PRODUCT.md`](PRODUCT.md) | Brand, users and design principles (short form) |
| [`documentation/ARCHITECTURE.md`](documentation/ARCHITECTURE.md) | Original design rationale for auth, response envelopes and service boundaries |

## Project structure

```
InterviewIQ/
├── backend/                 Spring Boot API (Maven)
│   ├── db/migrations/       Hand-run SQL migrations
│   └── src/main/java/com/interviewiq/
│       ├── config/  security/  controller/  service/  service/prompt/
│       ├── entity/  repository/  dto/  dto/ai/  mapper/  exception/
├── frontend/                React + Vite app
│   └── src/
│       ├── pages/  components/  layouts/  routes/
│       ├── services/        One API module per resource
│       ├── hooks/  lib/
│       └── index.css        Design tokens
├── documentation/
│   └── ARCHITECTURE.md
├── documentation.md
├── Product-Requirement.md
├── Design-System.md
└── PRODUCT.md
```

## Known gaps

- No migration tool yet (Flyway/Liquibase); migrations are hand-run SQL.
- AI generation runs synchronously; there's no background job queue.
- No frontend tests, and no committed ESLint config.
- No rate limiting on AI endpoints.
