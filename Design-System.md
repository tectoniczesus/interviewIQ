# InterviewIQ Design System

This is the reference for building UI in `frontend/`. It describes the system **as it ships today**. The source of truth is the code:

| What | Where |
|---|---|
| Color tokens, fonts, easing | `frontend/src/index.css` (`:root`) |
| Tailwind mapping of tokens | `frontend/tailwind.config.js` |
| Component classes (`.card`, `.btn-primary`, `.field`, …) | `frontend/src/index.css` (`@layer components`) |
| Page hues, page header, app icon tiles | `frontend/src/components/PageHeader.jsx` |
| Score tiers and the score ring | `frontend/src/components/ScoreCard.jsx` |
| Stage colors and tracker rules | `frontend/src/lib/applications.js` |
| Skeletons | `frontend/src/components/Skeleton.jsx` |
| Icons | `frontend/src/components/icons.jsx` |

> **Note on `DESIGN.md`.** `DESIGN.md` documents an earlier "Marked-Up Draft" direction: warm paper, an ochre marking color, Space Grotesk and IBM Plex. The app has since moved to an Apple-style minimal theme, with a cool grey ground, blue accent, system fonts and pill buttons. The **principles** in `DESIGN.md` still hold: verdict colors, figures before reasons, two shadows, motion only on state change. Its **hex values and typefaces** do not. Where the two disagree, this file and the code win.

---

## 1. Principles

1. **One clear next action per screen.** A page has one primary button. Everything else is secondary or a link.
2. **Number first, then the reason.** A score is always shown next to its verdict word and its evidence.
3. **Calm density.** White cards on a grey ground, generous spacing, no widget sprawl.
4. **Color means something.** Green, amber and red are verdicts. Blue is "you can act here". A page hue is a wayfinding color, nothing more.
5. **AI output is provisional.** Loading, empty and failure states are designed with every panel, not added later.
6. **Motion explains a change.** Nothing loops (except a skeleton pulse) and nothing replays on refetch.

**Anti-references:** generic AI-purple SaaS (gradients, sparkles, glass), dense enterprise dashboards, stock-photo recruiting.

---

## 2. Color

All colors are CSS variables holding RGB channels, so Tailwind opacity modifiers work (`bg-accent/10`, `text-ink/70`). **Never add a new hex in a component.** Use a token, or a token at opacity.

### 2.1 Core tokens

| Token | Tailwind | Hex | Use |
|---|---|---|---|
| `paper` | `bg-paper` | `#F5F5F7` | Page ground, drawer body, icon chips inside cards, text on ink |
| `surface` | `bg-surface` | `#FFFFFF` | Cards, fields, secondary buttons |
| `ink` | `text-ink` / `bg-ink` | `#1D1D1F` | Body text and headings, primary button, ink slabs, scrims (15.5:1) |
| `ash` | `text-ash` | `#6E6E73` | Secondary text, captions, eyebrows, placeholders, idle icons (4.7:1 on paper) |
| `line` | `border-line` | `#D2D2D7` | Borders, dividers, bar and skeleton tracks |
| `accent` | `text-accent` | `#0066CC` | Links, focus ring, progress fills, hover borders, selection (5.1:1) |

### 2.2 Verdict (state) colors

| Token | Hex | Meaning |
|---|---|---|
| `verdant` | `#1F7A37` | Good / done: score ≥ 80, Offer, completed, `COMPLETED` |
| `ochre` | `#A04800` | Middle / in progress: score 50–79, Interviewing, `PROCESSING`, warnings |
| `ochre-deep` | `#A04800` | Alias of `ochre`, kept for older call sites. Prefer `ochre` in new code |
| `brick` | `#C4001A` | Bad / attention: score < 50, Rejected, errors, overdue, destructive actions |

Each passes 4.5:1 as text inside its own 10% tint pill on white.

### 2.3 On-ink variants

On an ink slab (the Dashboard hero, the cover-letter composer), switch to the lighter tones:

| Token | Hex | Contrast on ink |
|---|---|---|
| `accent-on-ink` | `#2997FF` | 5.6:1 |
| `verdant-on-ink` | `#30D158` | 8.3:1 |
| `ochre-on-ink` | `#FF9F0A` | 8.2:1 |
| `brick-on-ink` | `#FF6961` | 6.0:1 |

Text on ink is `text-paper` at 100%, `/75` or `/60`. Readable text never goes below `paper/50`. Use 10–40% only for dividers, idle icons and dashed outlines.

### 2.4 Page hues

Each tool page owns one hue, used iOS-Settings style. The hue appears as a solid app-icon tile, a 10% tint behind the page header, the icon chip in list rows, and the page's primary button. White text on each hue passes 4.5:1.

| Hue | Token | Hex | Page |
|---|---|---|---|
| Blue | `hue-blue` | `#0066CC` | Resumes |
| Teal | `hue-teal` | `#0B6E79` | Jobs |
| Orange | `hue-orange` | `#B8440F` | Interview Prep |
| Gold | `hue-gold` | `#8A6500` | Roadmaps (list and detail) |
| Pink | `hue-pink` | `#C2185B` | Cover Letters |

Teal and orange were chosen over green and amber on purpose, because green and amber already mean "good" and "middle" scores. Dashboard and Applications have no hue: they use ink and the stage colors. Use the `HUES` map in `PageHeader.jsx`. It holds full class strings so Tailwind's JIT keeps them; never build hue classes dynamically.

### 2.5 Chrome (sidebar)

| Token | Hex | Use |
|---|---|---|
| `chrome` | `#FBFBFD` | Sidebar and mobile drawer ground |
| `chrome-ink` | `#1D1D1F` | Sidebar text |
| `chrome-muted` | `#6E6E73` | Sidebar secondary text |

### 2.6 Opacity rules
- **10%** of a verdict color behind a pill on white (`bg-verdant/10 text-verdant`).
- **5%** behind a banner that sits on the grey ground, with a **20–30%** border.
- **60%** of `line` for the neutral pill fill; **70%** for skeleton blocks.
- **30%** ink scrim behind the detail drawer; **40%** behind the mobile nav.

---

## 3. Typography

| Role | Stack (CSS var) |
|---|---|
| Display (`font-display`) | `-apple-system, BlinkMacSystemFont, "SF Pro Display", "Instrument Sans"` |
| Text (`font-sans`) | `-apple-system, BlinkMacSystemFont, "SF Pro Text", "Instrument Sans"` |
| Figures (`font-mono`) | Same as text, with `font-variant-numeric: tabular-nums` |
| Code (`font-code`) | Same as text |

On Apple devices this renders as SF Pro. Elsewhere it falls back to **Instrument Sans**, which is loaded from Google Fonts in `index.html`. `font-mono` is not a monospace face. It is the text face with tabular numerals, so columns of figures line up.

### Scale

| Style | Classes | Use |
|---|---|---|
| Hero greeting | `font-display text-3xl sm:text-[2.75rem] leading-tight` | Dashboard greeting only |
| Page title | `font-display text-2xl font-semibold leading-tight` | One `h1` per page (inside `PageHeader`) |
| Section title | `font-display text-base font-semibold` | Section headings on a page |
| Card title | `text-sm font-semibold` | Heading inside a card or board column |
| Lead | `text-base` (`max-w-prose`) | First line under a title, long summaries |
| Body | `text-sm` | Default UI and reading text |
| Caption | `text-[13px] text-ash` | Second line of a row: date, company, time ago |
| Label | `text-sm font-medium` | Buttons, row titles, active nav |
| Small label | `text-xs font-medium` | Pills, compact buttons |
| Eyebrow | `.eyebrow` = `text-xs font-medium uppercase tracking-wide text-ash` | Field labels, stat labels, group labels |
| Figure | `font-mono font-semibold leading-none` + size | Scores, counts, percentages |
| Data | `font-mono text-xs` | "3/5 done", "2d in stage", score inside a pill |

### Rules
- **Weights:** 400, 500 and 600 only. Headings and figures are 600.
- **Tracking:** `h1`–`h3` get `-0.015em`, and `h1` gets `-0.025em` (set in `@layer base`). The only positive tracking is the uppercase eyebrow.
- **Every data number uses `font-mono`.** That covers scores, counts, percentages and day counts. Never use it for prose or labels.
- Use `text-balance` on titles and `text-pretty` on descriptions.

---

## 4. Layout and spacing

### Shell (`layouts/MainLayout.jsx`)
- **≥ 1024px (`lg`):** a 240px (`w-60`) sticky sidebar on `bg-chrome` with a right hairline. The page scrolls beside it.
- **< 1024px:** the sidebar becomes a 256px (`w-64`, max 80vw) drawer over a `bg-ink/40` scrim and slides in over 200ms. A sticky top bar holds a 40px menu button, the wordmark and the account button.
- Nav order: Dashboard, Resumes, Jobs, Applications, Roadmaps, Cover Letters, Interview Prep.

### Content widths
| Width | Use |
|---|---|
| `max-w-3xl` (768px) | Reading and form pages |
| `max-w-6xl` (1152px) | Dashboard |
| `max-w-7xl` (1280px) | Application board |

Page padding is 16px on phones, 24px from `sm` and 32px from `lg`. There are only two breakpoints: **`sm` 640px** and **`lg` 1024px**.

### Spacing (4px scale)
| Gap | Use |
|---|---|
| 32px (`space-y-8`) | Between page sections |
| 24px | Page header to content |
| 12–16px | Between stacked cards |
| 12px | Section heading to content; items in a row |
| 8px | List rows; paired buttons |
| 16 → 20px | Card padding (`p-4 sm:p-5`) |
| 2/6/10px | Only inside pills, chips and tight rows |

### Responsive behavior
- On phones, header rows stack and primary actions go full width (`w-full sm:w-auto`).
- Grids step up rather than squeeze. Stat tiles go from 2 to 4 columns, recent lists from 1 to 2, quick actions from 1 to 2 to 4.
- The page never scrolls sideways. The board scrolls horizontally inside its own container, with columns at least 240px wide.

---

## 5. Shape and elevation

### Radius
| Radius | Use |
|---|---|
| 4px (`rounded`) | Checkboxes, segments |
| 6px (`rounded-md`) | Fields, icon buttons, nav items |
| 8px (`rounded-lg`) | Rows and tiles inside a card, banners |
| 10px | Hue chips (`rounded-[10px]`) |
| 12px (`rounded-xl`) | Cards, board columns, empty states |
| 16px (`rounded-2xl`) | Page header panel, ink slabs |
| 28% | App-icon tiles (squircle) |
| Full | Buttons, pills, dots, bars, rings, toast |

### Elevation
Cards have **no visible border**. The hairline is drawn by the shadow:

| Name | Value | Use |
|---|---|---|
| Card (rest) | `0 0 0 1px rgb(0 0 0 / .05), 0 1px 2px rgb(0 0 0 / .04)` | `.card`, `.card-link` |
| Card (hover) | `0 0 0 1px rgb(0 0 0 / .06), 0 8px 24px rgb(0 0 0 / .08)` | `.card-link:hover`, floating things |
| `shadow-card` / `shadow-card-hover` | Tailwind utilities with an ink tint | Skeleton rows, drawer, suggestion card, toast |

A deeper shadow means exactly one of two things: the element is clickable and the pointer is on it, or the element floats above the page. The sticky top bar uses the only backdrop blur in the system.

A **dashed border** always means "nothing here yet". It marks empty states, empty board columns and a score ring with no score.

---

## 6. Components

### 6.1 Buttons (`index.css`)

| Class | Look | Use |
|---|---|---|
| `.btn-primary` | Ink fill, paper label, pill, `min-h-10`, `px-4 py-2 text-sm font-medium`; hover `ink/85` | The one main action |
| `.btn-primary` + `HUES.<hue>.button` | Page-hue fill, white label | Primary action on a hued tool page |
| `.btn-secondary` | White, `line` border, ink label, pill; hover border `ink/30` | Secondary actions, Retry |
| `.btn-ghost-danger` | No fill, `brick/80` label, `text-xs`; hover `brick/10` fill | Delete |

- **Motion:** with a fine pointer, hover lifts the button 1px. Press moves it 1px down at `scale(.985)`. Colors ease over 120–150ms using `--ease`.
- **Disabled:** 40% opacity with `cursor-not-allowed`.
- **Busy:** the button keeps its place and swaps its label for a verb with an ellipsis ("Analyzing…", "Saving…").
- **On ink:** invert the button to a paper fill with an ink label.
- **Text links:** accent color with an underline. Quiet links ("View all →") are `text-ash hover:text-ink`.

### 6.2 Fields

- `.field`: white, `line` border, `rounded-md`, `px-3 py-2 text-sm`, ash placeholder. On focus the border goes to `ink/30` and the outline is removed (the one exception to the global focus ring).
- `.field-on-ink`: for selects and inputs on an ink slab. The fill is `white/10` with a `paper/20` border, focus uses `accent-on-ink`, and it draws its own chevron.
- **Labels:** an `.eyebrow` above the field with a 6px gap, or an inline label for a single select ("Compare against:"). A placeholder is never the only label.
- **Errors:** show a brick banner below the form, not inline field decoration.
- **Checkbox:** 20px with 4px corners, checked in `verdant`.

### 6.3 Cards

| Variant | Recipe |
|---|---|
| `.card` | `rounded-xl bg-surface p-4 sm:p-5` + rest shadow |
| `.card-link` | Same surface. With a fine pointer it lifts 2px on hover (180ms in, 120ms out) and takes the hover shadow |
| Clickable row | `.card-link` + `HueChip` (36px) + `text-sm font-medium` title + 13px ash caption + right-aligned pill + 16px chevron (`.dash-arrow`, slides 3px on hover) |
| Attention card | 3px left edge in `brick/70` + brick reason under the title ("Follow up today") |
| Emphasized card | `border-accent/40`, optional `bg-accent/5`; marks the one current item |

### 6.4 Page header (`PageHeader.jsx`)

Every tool page opens with one tinted panel (`rounded-2xl p-5 sm:p-6`, a 10% hue tint on white). It holds a 48px `AppIcon` tile, the `h1`, a one-line description (`text-ink/70`), an optional fact on the right from `sm` up, and then the page's main tool (form or drop zone) as `children`.

```jsx
<PageHeader icon={BriefcaseIcon} hue="teal" title="Jobs"
            description="Save the roles you're targeting.">
  <form>…</form>
</PageHeader>
```

### 6.5 Pills and badges

All pills are `rounded-full px-2 py-0.5 text-xs font-medium`, with a 10% tint behind full-strength text.

**Stage pill** (`StagePill.jsx`, colors in `lib/applications.js`). A 6px dot sits before the label.

| Stage | Pill | Dot |
|---|---|---|
| Saved | `bg-line/60 text-ash` | `ash/60` |
| Applied | `bg-ink/[0.07] text-ink` | `ink` |
| Interviewing | `bg-ochre/10 text-ochre` | `ochre` |
| Offer | `bg-verdant/10 text-verdant` | `verdant` |
| Rejected | `bg-brick/10 text-brick` | `brick` |
| Withdrawn | `bg-line/60 text-ash` | `ash/40` |

**Processing badge** (`StatusBadge.jsx`): `UPLOADED` neutral, `PROCESSING` ochre, `COMPLETED` verdant, `FAILED` brick.

**Score pill:** a mono number ("ATS 82", "76%") toned by score tier.

**Skill chip:** a pill plus a 1px border at 20–30% of its own color. Long words wrap.

### 6.6 Score ring (`ScoreCard.jsx`)

The product's signature element.

| | Default | Hero |
|---|---|---|
| Size | 96px | 128px |
| Stroke | 8px | 10px |

- A round-capped arc starts at twelve o'clock, on a track of its own color at 15%.
- The number sits in the center in mono semibold. Below the ring are an eyebrow label and the verdict word.
- **Tiers:** `≥ 80` is verdant "Strong", `50–79` is ochre "Needs work", `< 50` is brick "Weak". Use `tier(score)` and never re-derive the thresholds.
- On ink (`onDark`), use the `*-on-ink` tones.
- The arc fills once over 200ms on mount, and not at all under reduced motion.
- With no score, show a dashed circle and a link to create one.

### 6.7 Ink slab

Ink slabs are dark, `rounded-2xl` blocks with paper text. There are exactly two:
- **Dashboard hero:** a date line (`font-code text-xs uppercase text-accent-on-ink`), the greeting, a status line, an inverted button, and the hero score ring behind a `paper/10` divider. Below these sits a strip of stat tiles (2 columns, 4 from `sm`) separated by hairlines.
- **Cover-letter composer:** the resume and job pickers (`.field-on-ink`) and the generate action.

### 6.8 Proportional bars
- **Pipeline bar:** 12px tall, `rounded-full`, stage-colored segments with 2px gaps on a `line/60` track. It wipes in once (`.bar-reveal`, 450ms). The legend repeats each stage as a dot, a label and a mono count.
- **Coverage bar:** 8px. A verdant fill on a `brick/20` track, with the label on the left and "4 of 6 · 67%" in mono on the right.
- **Progress bar** (`ProgressBar` in `components/roadmap.jsx`): 6–10px on a `line` track, with `role="progressbar"`. The fill is the page hue (`hue-gold` on roadmaps) and turns verdant at 100%.

### 6.9 Application board (`pages/Applications.jsx`)
- **Column:** a `line/30` ground, `rounded-xl`, at least 240px wide. The header shows a stage dot, the title and a mono count. While a card is dragged over it, the column takes a `border-accent/60` border and a `bg-accent/5` tint. An empty column shows a dashed "Drop a card here".
- **Card:** title, company caption, score pill, "Nd in stage" in mono, a follow-up line, then a compact stage select. A card being dragged drops to 40% opacity. When a card arrives in a column it settles in (`.card-settle`, a 6px rise over 180ms). Nothing animates on first load.
- **Detail drawer:** up to 448px wide, slides in from the right (`.drawer-in`, 220ms) over a `bg-ink/30` scrim (`.scrim-in`). The header has the title, the stage pill and a 40px close button. The body is paper, divided into eyebrow-headed sections. A timeline runs down the left as a 1px rule with 8px dots.

### 6.10 Stage suggestion and toast (`StageSuggestion.jsx`)

The suggestion is a card up to 384px wide with the hover shadow. It is pinned bottom-right, or bottom-center on phones. It asks one question and offers "Not yet" (secondary) next to the move (primary). After a move, an ink pill toast confirms it for 3 seconds.

### 6.11 Roadmap timeline (`components/roadmap.jsx`, `pages/LearningRoadmap.jsx`)

A vertical 2px rail with 32px round week nodes:

| Week | Node |
|---|---|
| Done | Solid `hue-gold` fill with a white check; the rail below it turns `hue-gold/50` |
| Current | White with a 2px `hue-gold` border, a `hue-gold` mono number and a 4px `hue-gold/15` halo; its card takes a `hue-gold/40` border |
| Upcoming | `line` border with an ash number |

Task-type chips: `LEARN` is `bg-ink/5 text-ink/70`, `PRACTICE` is ochre and `BUILD` is verdant. A completed task row turns to a `verdant/30` border with a `verdant/5` fill, and its title is struck through in ash.

### 6.12 Feedback states

Every data panel ships all four states:

| State | Pattern |
|---|---|
| Loading | `Skeleton`, `SkeletonRow`, `SkeletonList`, `SkeletonRing`, `SkeletonCard`. Each copies the shape of the content it stands in for. `bg-line/70`, `animate-pulse`. On ink, use `!bg-ink/10` or `paper/15` |
| Empty | Dashed 1px border, `rounded-xl`, `bg-surface/60`, centered. A 40px round paper badge with an icon, one ash sentence and a link to the next step |
| Error | `rounded-lg border border-brick/20 bg-brick/5 text-sm text-brick` with a compact `.btn-secondary` "Retry" on the right. A non-critical panel uses the same shape in white with ash text |
| Busy AI action | The button label becomes a verb with an ellipsis. On Resumes, the page-thumbnail lines "write" in sequence (`.page-lines[data-busy]`) |

Never use plain "Loading…" text in place of a skeleton.

### 6.13 Icons (`icons.jsx`)

A small custom set on a 24px grid with a 1.75 stroke, round caps and joins, drawn in `currentColor`. Sizes are 16px (chevrons, plus), 18px (chips, stat tiles) and 20px (empty-state badges). Icons are decorative: they always sit beside visible text and carry `aria-hidden`.

---

## 7. Motion

All motion uses transform, opacity or clip-path only.

| Token | Value |
|---|---|
| `--ease` | `cubic-bezier(.2, .8, .2, 1)`: hovers, presses |
| `--ease-enter` | `cubic-bezier(.22, 1, .36, 1)`: arrivals |

| Class | Duration | What it explains |
|---|---|---|
| `.fade-in` | 150ms | Simple appearance |
| `.card-settle` | 180ms | A card arrived in a new column |
| `.scrim-in` | 180ms | Overlay opened |
| `.drawer-in` | 220ms | Detail drawer opened |
| `.disclose` | 300ms | Expand/collapse (grid rows `0fr → 1fr`, no height animation) |
| `.bar-reveal` | 450ms | Pipeline bar's first draw |
| `.letter-reveal` | 600ms | A generated cover letter arriving, top-down |
| `.ring-fade` | 1400ms | Highlights the row that was just added, once |
| `.dash-enter` | 640ms + 110ms stagger | Dashboard hero entrance, once per mount |
| `.stagger` | 40ms per item | A list's first arrival; added once, never on refetch |

**Rules**
- Hover movement applies only under `(hover: hover) and (pointer: fine)`.
- Nothing replays on a refetch. Elements stay mounted, and stagger classes are added once.
- `prefers-reduced-motion: reduce` collapses every animation and transition to 0.01ms, removes transforms and stops the skeleton-style loops. This is handled globally in `index.css`; don't re-implement it per component.

---

## 8. Accessibility

- **Contrast:** every text and background pair in the token table passes WCAG AA (4.5:1). Check before adding a new combination, especially colored text on a tint sitting directly on the grey ground.
- **Focus:** a global `*:focus-visible` outline in 2px `accent` with a 2px offset. Don't remove it, except on `.field` as documented.
- **Targets:** interactive elements are at least 40px tall (`min-h-10`, `h-10 w-10` icon buttons). Raise the remaining 36px controls (`.btn-ghost-danger`, the drawer close button).
- **Color is never the only signal.** Pair it with a word, a number or an icon.
- **Labels:** every field has a visible or screen-reader-only label.
- **Dialogs and drawers:** move focus in, trap Tab, close on Escape and restore focus on close.
- **Landmarks:** the page header is a `section` labelled by `#page-title`. Skeleton lists use `role="status" aria-label="Loading"`.
- **Open items:** Jobs form labels, mobile nav focus handling, per-route `document.title`, a skip link and sidebar tagline contrast.

---

## 9. Do and don't

**Do**
- Use tokens (`bg-paper`, `text-ash`, `border-line`, `text-accent`) and opacity modifiers.
- Put every data number in `font-mono`.
- Use `tier(score)` for score color and verdict, and `STAGES` for stage color.
- Give each tool page one hue through `PageHeader` and `HUES`.
- Design loading, empty and error states alongside the happy path.
- Keep one primary button per view.

**Don't**
- Don't use Tailwind's default palette (`gray-500`, `blue-600`, `red-500`) or raw hexes in components.
- Don't use verdict colors (`verdant`, `ochre`, `brick`) for decoration, or page hues to mean good or bad.
- Don't add gradients, purple, sparkles, or glass and blur. The top bar's blur is the only one.
- Don't use weights above 600 or below 400.
- Don't add a third shadow level, or animate on repeat loads.
- Don't build Tailwind classes dynamically (`` `bg-hue-${name}` ``). JIT will drop them.
- Don't add a third ink slab.

---

## 10. Adding to the system

1. **New color:** add the RGB channels to `:root` in `index.css`, map the token in `tailwind.config.js`, document its contrast here, then use it.
2. **New component class:** add it under `@layer components` in `index.css` so page utilities can still override it.
3. **New tool page:** pick a hue that doesn't collide with a verdict color, add it to `HUES` and `:root`, and open the page with `PageHeader`.
4. **New motion:** state what change it explains, keep it under 300ms unless it's a one-time arrival, use transform, opacity or clip-path only, and make sure the global reduced-motion rule covers it.
5. Update this file in the same change.
