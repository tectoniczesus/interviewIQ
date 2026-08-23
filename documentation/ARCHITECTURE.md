# Architecture Notes

## Auth: Clerk JWT verification

Rather than calling out to Clerk on every request, the backend fetches
Clerk's JWKS once (`ClerkJwkProvider`), caches the public keys by `kid` for
an hour, and verifies each incoming session token's signature locally
(`ClerkJwtValidator`). `ClerkAuthenticationFilter` runs before Spring
Security's standard auth filter and populates the `SecurityContext` with an
`AuthenticatedUser` record containing only what the rest of the app needs:
the Clerk user id and email. Every service resolves "who is calling" via
`CurrentUserProvider`, never from a client-supplied id.

## Two response envelopes, on purpose

The spec describes two response shapes:

- Section 27 (`{"success": true, "data": {...}}`) for successful
  controller responses — implemented as `dto.ApiResponse<T>`.
- Section 23 (`{timestamp, status, error, message, path}`) for errors —
  implemented as `exception.ErrorResponse`, returned exclusively by
  `GlobalExceptionHandler`.

Splitting them this way means controllers only ever think about the
success case, and every failure — whatever layer it originated in — goes
through one place that guarantees no stack trace, exception class name, or
internal detail leaks to the client.

## Service layer boundaries around external APIs

`GroqService` and `FirecrawlService` are deliberately "dumb" — they know
how to make an HTTP call and return raw text, nothing about resumes or
jobs. Prompt construction (`ResumeAnalysisPromptBuilder`,
`JobMatchPromptBuilder`, etc.) and JSON-schema validation are the
responsibility of feature-specific services added in Phases 3, 5, 6, and 7.
This keeps the "don't let the LLM return arbitrary unstructured text"
requirement enforceable in one place per feature, rather than scattered
across prompt strings embedded in controllers.

Similarly, `SupabaseStorageService` is the only component allowed to talk
to the Supabase Storage REST API — resume PDFs are stored there, never in
Postgres, and only the storage *path* is persisted on the `Resume` entity.

## Why entities/repositories exist already, if this is "just" Phase 1

The full schema was scaffolded now (rather than incrementally per phase)
because Phase 2 onward all depend on the same handful of tables, and JPA
entities are cheap to define but easy to get subtly wrong (missing indexes,
wrong fetch types, wrong cascade behavior) if bolted on under feature
pressure later. The entities in this scaffold contain no business logic —
that's still added phase-by-phase in the `service` layer.

## Known gaps, intentionally deferred

- No Flyway/Liquibase migration yet — `ddl-auto` is `validate` by default
  precisely so nobody points this at a real Supabase database with
  `update`/`create` by accident. Add a migration tool before Phase 2.
- No async/job-queue infrastructure for long-running AI calls yet (spec
  section 26 asks for the backend to be *structured so this can be added
  without a redesign* — the service-layer boundaries above are what make
  that possible, but nothing async is wired up yet).
- No tests yet — that's Phase 10.
