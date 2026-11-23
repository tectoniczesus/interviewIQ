# Product

## Register
product (Operate mode: users complete tasks; scanability and consistency outrank expression)

## Platform
web (React 18 + Vite + Tailwind, Clerk auth), responsive down to phone width

## What it is
InterviewIQ is an AI career assistant. Users upload a resume and get an ATS analysis, compare it against target jobs, generate tailored cover letters, prepare for interviews, build learning roadmaps for skill gaps, and track applications through a pipeline.

## Users
A broad mix of job seekers (active searchers, new grads, career switchers) with no single primary persona. Design for people mid-task who want a clear next step, often under time pressure.

## Brand personality
Sharp and ambitious. Confident, performance-driven, direct. Scores and feedback are honest and specific, not padded.

## Anti-references
- Generic AI-purple SaaS (gradients, sparkles, glassmorphism)
- Dense enterprise dashboards (heavy tables, tiny type, widget sprawl)
- Corporate stock-photo recruiting look (handshakes, suits, blue-and-white)

## Design principles
1. One clear next action per screen; the pipeline from resume to job to match to prep should always show where the user is.
2. Show the number, then the reason. Scores always come with the specific evidence behind them.
3. Calm density: enough information to decide, never a wall of widgets.
4. Brand lives in precise details (type, color restraint, microcopy), not decoration.
5. AI output is provisional: loading, failure and retry states are first-class, not afterthoughts.

## Incumbent visual identity
Open to change; treat as a starting point, not a constraint. Current system, as built:
- **Color:** warm paper background (#F6F7F3), ink (#1C2430), ochre accent (#B8842A) for fills, rings and borders only, `ochre-deep` (#8A6218) for any small ochre text, verdant for done/good, brick for attention/bad. Ink is also the sidebar and the Dashboard hero slab; on dark surfaces use the lighter tier tones (#5FBF97, #E0B15A, #E8806C).
- **Type:** Space Grotesk (display), IBM Plex Sans (body), IBM Plex Mono (figures, tabular). Page headings 2xl/3xl with tight tracking, section headings base, body sm/base, secondary text 13px. Large figures are mono with tight leading.
- **Surfaces:** white bordered cards with soft shadows; proportional bars and single divided panels preferred over many separate boxes.
- **Motion:** short and explanatory only (state changes, arrivals); nothing animates on repeat loads. Reduced motion is respected globally.

## Accessibility
- Target WCAG AA. Small text needs 4.5:1: never use plain ochre for text under 18px, use `ochre-deep`.
- Interactive targets 40px or larger; some existing controls are 36px and should be raised.
- Every form field has a label (visible or screen-reader only), not a placeholder alone.
- Dialogs and drawers move focus in, trap Tab, and restore focus on close.
- Visible focus rings and reduced-motion support already exist globally.
- Known open items from the last audit: Jobs form labels, mobile nav drawer focus handling, per-route page titles, skip link, sidebar tagline contrast.

## Assumptions
Written without Impeccable's init template (the reference file was not readable in this session). Register, platform and principles were inferred from the repo and the user's answers. The visual identity and accessibility sections were refreshed from the code after a session of design work; edit freely.
