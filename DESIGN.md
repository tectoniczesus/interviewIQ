---
name: InterviewIQ
description: AI career assistant for resume analysis, job matching, interview prep and application tracking.
colors:
  ink: "#1C2430"
  ochre: "#B8842A"
  ochre-deep: "#8A6218"
  verdant: "#2F7A5C"
  brick: "#B14632"
  verdant-on-ink: "#5FBF97"
  ochre-on-ink: "#E0B15A"
  brick-on-ink: "#E8806C"
  paper: "#F6F7F3"
  white: "#FFFFFF"
  ash: "#616B78"
  line: "#E2E4DE"
typography:
  display:
    fontFamily: "Space Grotesk, system-ui, sans-serif"
    fontSize: "2.25rem"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "-0.025em"
  headline:
    fontFamily: "Space Grotesk, system-ui, sans-serif"
    fontSize: "1.875rem"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "-0.025em"
  title:
    fontFamily: "Space Grotesk, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 600
    lineHeight: 1.5
    letterSpacing: "-0.025em"
  title-sm:
    fontFamily: "Space Grotesk, system-ui, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 600
    lineHeight: 1.43
  body-lg:
    fontFamily: "IBM Plex Sans, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
  body:
    fontFamily: "IBM Plex Sans, system-ui, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.43
  caption:
    fontFamily: "IBM Plex Sans, system-ui, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 400
  label:
    fontFamily: "IBM Plex Sans, system-ui, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 500
    lineHeight: 1.43
  label-sm:
    fontFamily: "IBM Plex Sans, system-ui, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 500
    lineHeight: 1.33
  eyebrow:
    fontFamily: "IBM Plex Sans, system-ui, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 500
    lineHeight: 1.33
    letterSpacing: "0.025em"
  figure-lg:
    fontFamily: "IBM Plex Mono, ui-monospace, monospace"
    fontSize: "3rem"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "-0.025em"
    fontFeature: "'tnum'"
  figure-md:
    fontFamily: "IBM Plex Mono, ui-monospace, monospace"
    fontSize: "1.875rem"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "-0.025em"
    fontFeature: "'tnum'"
  figure-sm:
    fontFamily: "IBM Plex Mono, ui-monospace, monospace"
    fontSize: "1.5rem"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "-0.025em"
    fontFeature: "'tnum'"
  data:
    fontFamily: "IBM Plex Mono, ui-monospace, monospace"
    fontSize: "0.75rem"
    fontWeight: 400
    lineHeight: 1.33
    fontFeature: "'tnum'"
rounded:
  DEFAULT: "4px"
  md: "6px"
  lg: "8px"
  xl: "12px"
  "2xl": "16px"
  full: "9999px"
spacing:
  "1": "4px"
  "2": "8px"
  "3": "12px"
  "4": "16px"
  "5": "20px"
  "6": "24px"
  "8": "32px"
components:
  button-primary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
    typography: "{typography.label}"
    rounded: "{rounded.md}"
    padding: "8px 16px"
    height: "40px"
  button-primary-hover:
    backgroundColor: "rgb(28 36 48 / 0.85)"
  button-secondary:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    rounded: "{rounded.md}"
    padding: "8px 12px"
    height: "40px"
  button-ghost-danger:
    textColor: "rgb(177 70 50 / 0.8)"
    typography: "{typography.label-sm}"
    rounded: "{rounded.md}"
    padding: "6px 12px"
    height: "40px"
  button-ghost-danger-hover:
    backgroundColor: "rgb(177 70 50 / 0.1)"
    textColor: "{colors.brick}"
  field:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    padding: "8px 12px"
  card:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    rounded: "{rounded.xl}"
    padding: "20px"
  pill-neutral:
    backgroundColor: "rgb(226 228 222 / 0.6)"
    textColor: "{colors.ash}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.full}"
    padding: "2px 8px"
  pill-good:
    backgroundColor: "rgb(47 122 92 / 0.1)"
    textColor: "{colors.verdant}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.full}"
    padding: "2px 8px"
  pill-warn:
    backgroundColor: "rgb(184 132 42 / 0.1)"
    textColor: "{colors.ochre-deep}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.full}"
    padding: "2px 8px"
  pill-bad:
    backgroundColor: "rgb(177 70 50 / 0.1)"
    textColor: "{colors.brick}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.full}"
    padding: "2px 8px"
  nav-item:
    textColor: "rgb(246 247 243 / 0.6)"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    padding: "8px 12px"
  nav-item-hover:
    backgroundColor: "rgb(255 255 255 / 0.05)"
    textColor: "rgb(246 247 243 / 0.9)"
  nav-item-active:
    backgroundColor: "rgb(255 255 255 / 0.1)"
    textColor: "{colors.paper}"
    typography: "{typography.label}"
  hero-slab:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
    rounded: "{rounded.2xl}"
    padding: "32px"
  score-ring:
    size: "96px"
  score-ring-hero:
    size: "128px"
  toast:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
    typography: "{typography.body}"
    rounded: "{rounded.full}"
    padding: "8px 16px"
---

# Design System: InterviewIQ

## Overview

**Creative North Star: "The Marked-Up Draft"**

InterviewIQ looks like a resume printed on warm paper and handed back by an editor who doesn't flatter. The page is paper, the writing is blue-black ink, and a single ochre marking pen does the pointing: an underline under a link, a ring around a score, a tick beside the current page. Green and red are the editor's verdicts, and they appear only where something has actually been judged.

The mood is sharp, calm and exacting. Sharp, because figures are large, set in mono and placed before their explanation. Calm, because there is one sheet of paper, white cards with hairline borders, one slab of ink at the top of the Dashboard, and a 32px breath between sections. Exacting, because every number is tabular, every status color carries a word, and nothing moves unless a state changed.

The system stays away from the generic AI-SaaS look (purple, gradients, sparkles, glass), from dense enterprise dashboards, and from stock-photo recruiting imagery.

**Key Characteristics:**
- A warm paper page, white bordered cards, and ink reserved for the sidebar and the Dashboard hero.
- One accent color, used as a mark and never as a fill.
- Three typefaces with fixed jobs: Space Grotesk for headings, IBM Plex Sans for reading, IBM Plex Mono for figures.
- Proportional bars and rings instead of charts, always with the number beside them.
- Borders define surfaces. Shadows are barely there.
- Motion is short (150 to 450ms), tied to a state change, and switched off under reduced motion.

## Colors

A paper-and-ink palette with one marking color and two verdict colors. Every lighter value is one of these at lower opacity.

### Primary
- **Blue-Black Ink** (#1C2430): body text and headings, the primary button, the sidebar, the Dashboard hero slab, and the confirmation toast. Also the tint of every shadow and scrim.

### Secondary
- **Marking Ochre** (#B8842A): the marking pen. It draws the active-nav marker, the 2px focus outline, link underlines, the middle-tier score arc, in-progress bar fills, bullets, the hover border on clickable cards, and the 10% tint behind in-progress pills. It is not a text color at small sizes (3.3:1 on white).
- **Deep Marking Ochre** (#8A6218): ochre when it has to be read. Use it for any ochre-family text under 18px: pill labels, inline links, the current-week number (5.5:1 on white).

### Tertiary
- **Proof Green** (#2F7A5C): done and good. Scores of 80 and up, the Offer stage, completed tasks, bars at 100%, matched coverage, and solid check badges with a white check.
- **Correction Brick** (#B14632): attention and bad. Scores under 50, Rejected, errors, overdue follow-ups, destructive actions, and the track behind a coverage bar.
- **Proof Green on Ink** (#5FBF97), **Marking Ochre on Ink** (#E0B15A), **Correction Brick on Ink** (#E8806C): the same three verdicts lightened for ink surfaces, such as the hero score ring and the date line above the greeting.

### Neutral
- **Warm Paper** (#F6F7F3): the page background, the drawer body, icon chips inside white cards, and text on ink.
- **White** (#FFFFFF): the surface of cards, fields and secondary buttons.
- **Pencil Ash** (#616B78): secondary text, meta lines, eyebrows, placeholders and idle icons (5.0:1 on paper, 5.4:1 on white).
- **Ruled Line** (#E2E4DE): every 1px border and divider, bar and skeleton tracks, the neutral pill fill at 60%, and board columns at 30%.

### Named Rules
**The Marking Pen Rule.** Ochre marks; it does not fill and it does not speak. Use it for lines up to 2px, rings, dots, bars and 10% tints. Nothing larger than a progress bar or a 32px node is filled with ochre, and no plain-ochre text goes below 18px.

**The Verdict Rule.** Green, ochre and red are judgments, never decoration. Scores map to three tiers: 80 and up is green ("Strong"), 50 to 79 is ochre ("Needs work"), under 50 is brick ("Weak"). A verdict color always arrives with a word or a number.

**The Tint Rule.** No new hexes. A lighter value is a palette color at opacity: 10% behind pills on white, 5% behind banners on paper, 20 to 30% for the border around a tinted banner. Colored text on a 10% tint passes AA on white but not on paper (green drops to 4.2:1), so tints that sit directly on paper use 5%.

**The On-Ink Rule.** On ink, verdict colors switch to their on-ink versions and text is paper at 100%, 70% or 60%. Readable text never drops below paper at 50% (4.7:1). Steps from 10% to 40% are for dividers, idle icons and dashed outlines.

## Typography

**Display Font:** Space Grotesk (with system-ui, sans-serif)
**Body Font:** IBM Plex Sans (with system-ui, sans-serif)
**Label/Mono Font:** IBM Plex Mono (with ui-monospace, monospace)

**Character:** A geometric grotesque with a slightly technical edge for headings, an engineered sans for reading, and its mono sibling for figures. The pairing reads as precise without turning cold.

### Hierarchy
- **Display** (600, 30px rising to 36px from 640px, line-height 1.25, -0.025em): the Dashboard greeting only.
- **Headline** (600, 24px rising to 30px from 640px, line-height 1.25, -0.025em): the page heading. One per page.
- **Title** (600, 16px, -0.025em): section headings on the page, such as "Application pipeline" or a roadmap week.
- **Title small** (600, 14px): the heading inside a card or a board column.
- **Body large** (400, 16px, line-height 1.5): the lead sentence under a page heading and long summaries, capped near 65 characters per line.
- **Body** (400, 14px, line-height 1.43): default reading and interface text.
- **Caption** (400, 13px): the second line of a row, such as time ago, company or follow-up date.
- **Label** (500, 14px): buttons, row titles and the active nav item.
- **Label small** (500, 12px): pills, compact buttons and inline toggles.
- **Eyebrow** (500, 12px, +0.025em, uppercase, Pencil Ash): field labels in the drawer, stat labels, and group labels above chip sets.
- **Figure** (mono 600, line-height 1, -0.025em, tabular): 48px for hero stats and overall progress (36px on phones), 30px for the score-ring number and row percentages, 24px for pipeline counts.
- **Data** (mono 400 to 500, 12px, tabular): counts, "3/5 done", "2d in stage", and the number inside a score pill.

### Named Rules
**The Figures-in-Mono Rule.** Every number that is data (a score, a count, a percentage, a day count) is IBM Plex Mono with tabular numerals. Mono is for measurement and never for labels or prose.

**The 600 Ceiling Rule.** Weights are 400, 500 and 600. Headings and figures are 600. Nothing is bold and nothing is light; hierarchy comes from family and size.

**The Tight-at-16 Rule.** Space Grotesk at 16px and above takes -0.025em tracking. The only positive tracking in the system is the uppercase eyebrow.

## Layout

The app is a two-part shell. From 1024px up, a 240px ink sidebar stays fixed on the left for the full height and the page scrolls beside it. Below 1024px the sidebar becomes a 256px drawer (never wider than 80% of the screen) that slides in from the left over an ink scrim, and a sticky top bar carries a 40px menu button, the wordmark and the account button.

Page content is centered at one of three widths: 768px for reading and form pages, 1152px for the Dashboard, and 1280px for the application board. Page padding is 16px on phones, 24px from 640px, and 32px from 1024px. Only two breakpoints are in use: 640px and 1024px.

Spacing follows a 4px scale, and the same few steps do most of the work:
- 32px between page sections.
- 24px from a page header to its content.
- 12 to 16px between stacked cards.
- 12px from a section heading to its content, and between items in a row.
- 8px between list rows and between paired buttons.
- Card padding is 16px, rising to 20px from 640px.
- Half steps (2px, 6px, 10px) appear only inside pills, chips and tight rows.

On phones, header rows stack and primary actions go full width. Grids step up rather than squeeze: stat tiles go from 2 to 4 columns, recent lists from 1 to 2, quick actions from 1 to 2 to 4. The page itself never scrolls sideways; the application board scrolls horizontally inside its own container, with columns at least 240px wide.

## Elevation & Depth

Depth is ambient and borders lead. A 1px Ruled Line border defines every card; the resting shadow is a 4 to 6% softening you should barely notice. Height is carried mostly by tone: paper for the page, white for cards, ink for the sidebar and hero. Both shadows are tinted with ink, never pure black.

### Shadow Vocabulary
- **card** (`box-shadow: 0 1px 2px rgb(28 36 48 / 0.04), 0 1px 3px rgb(28 36 48 / 0.06)`): the resting state of every card.
- **card-hover** (`box-shadow: 0 2px 4px rgb(28 36 48 / 0.05), 0 6px 16px rgb(28 36 48 / 0.08)`): a clickable card under the pointer, and anything that floats above the page (the detail drawer, the stage suggestion, the toast).

Overlays sit on an ink scrim: 30% behind the detail drawer, 40% behind the mobile navigation. The sticky top bar is paper at 95% with a backdrop blur so content stays legible as it scrolls under; that is the only blur in the system.

### Named Rules
**The Two Shadows Rule.** There are two shadows, card and card-hover. A deeper shadow means exactly one of two things: this is clickable and you are on it, or this floats above the page. Nothing heavier exists.

## Shapes

Radius grows with the size of the thing. Segments inside a segmented control and checkboxes are barely rounded (4px). Controls such as buttons, fields and nav items are gently rounded (6px). Rows and tiles that sit inside a card take 8px. Cards, board columns and empty states take 12px. The Dashboard hero slab alone takes 16px. Pills, dots, bars, rings, check badges and the toast are fully round.

Lines are 1px Ruled Line throughout, used as borders and as dividers inside a single panel. A dashed line always means "nothing here yet": empty states, an empty board column, a score ring with no score.

Icons are a small custom set drawn on a 24px grid with a 1.75 stroke, round caps and joins, and the current text color. They are shown at 16px (chevrons, plus), 18px (icon chips, stat tiles) and 20px (empty-state badges). They are decorative and always sit next to visible text.

## Components

Controls are tactile and confident: solid, clearly bounded, and obviously pressable. Where older pages (Jobs, Resumes, Cover Letters, Interview Prep, Resume Analysis, Job Match) still carry earlier patterns, this section is the reference.

### Buttons
- **Shape:** gently rounded (6px), at least 40px tall, 14px medium label, content centered.
- **Primary:** solid Blue-Black Ink with a Warm Paper label, 8px by 16px padding. Hover lightens the fill to ink at 85%.
- **Secondary:** white with a 1px Ruled Line border and an ink label, 8px by 12px padding. Hover darkens the border to ink at 30%.
- **Ghost danger:** no fill, brick label at 80%, 12px medium. Hover adds a 10% brick fill and full-strength text. Used for delete.
- **In a row:** inside dense rows and banners, primary and secondary take a 12px label and keep the 40px minimum height.
- **On ink:** the action inverts to a Warm Paper fill with an ink label; hover goes to white.
- **States:** color changes take 150ms. Disabled is 40% opacity. A busy button keeps its place and swaps its label for a verb with an ellipsis ("Saving…", "Retrying…").
- **Focus:** a 2px Marking Ochre outline with a 2px offset. This is the global focus ring; text fields are the one exception (see Inputs).
- **Text links:** ink, 14px medium, with an ochre underline at 50% that goes to full on hover. Quiet links ("View all →", "← Back to roadmaps") are Pencil Ash and turn ink on hover.

### Pills and chips
- **Style:** fully round, 2px by 8px padding, 12px medium, a 10% tint behind full-strength text. Four tones: neutral (Ruled Line at 60% with Pencil Ash), good (green), warn (ochre tint with Deep Marking Ochre text), bad (brick).
- **Stage pill:** adds a 6px dot in the stage color before the label. Saved and Withdrawn are ash, Applied is ink, Interviewing is ochre, Offer is green, Rejected is brick.
- **Score pill:** the number is mono and tabular ("ATS 82", "76%") and takes its tone from the score tier.
- **Skill chip:** adds a 1px border at 20 to 30% of its own color and wraps long words.

### Cards / Containers
- **Corner Style:** 12px.
- **Background:** white on the paper page.
- **Shadow Strategy:** `card` at rest; see Elevation & Depth.
- **Border:** 1px Ruled Line.
- **Internal Padding:** 16px, rising to 20px from 640px.
- **Clickable card:** the same surface with 12 to 16px padding. Hover moves the border to ochre at 40% and the shadow to `card-hover` over 150ms. A clickable row carries a 36px paper icon chip (8px corners), a 14px medium title, a 13px caption, a right-aligned pill, and a 16px chevron that slides 2px and turns ochre on hover.
- **Attention card:** a 3px left edge in Correction Brick at 70%, with the reason in brick under the title ("Follow up today", "No update in 16 days").
- **Emphasized card:** an ochre border at 40% marks the one card that is current, with an optional 5% ochre fill.
- **Empty state:** a dashed 1px border, 12px corners, white at 60%, centered content: a 40px round paper badge with an icon, one ash sentence, and an underlined link to the next step.
- **Banner:** 8px corners, a 1px brick border at 20%, a 5% brick fill, 14px brick text, and a compact secondary "Retry" on the right. A non-critical panel that failed uses the same shape in white with ash text.
- **Skeleton:** Ruled Line at 70%, 6px corners, a slow pulse. A skeleton copies the shape of what is loading (a row with icon chip, two lines and a pill; a ring; a card). On ink it is paper at 15%.

### Inputs / Fields
- **Style:** white, 1px Ruled Line border, gently rounded (6px), 8px by 12px padding, 14px ink text, Pencil Ash placeholder, full width.
- **Focus:** the border darkens to ink at 30%. Fields drop the global ochre outline.
- **Labels:** an eyebrow above the field with a 6px gap, or an inline label to the left of a single select ("Compare against:"). A placeholder is a hint and never the only label.
- **Select in a row:** inside a card row the select shrinks to its content width with 12px text (the stage menu on a board card).
- **Checkbox:** 20px with 4px corners, checked in Proof Green.
- **Error:** a brick banner below the form, not inline field decoration.

### Navigation
- **Sidebar:** 240px of ink with 24px by 16px padding. The wordmark is Space Grotesk 18px semibold in paper. Links are 14px with 8px by 12px padding and 6px corners. Default is paper at 60%; hover is paper at 90% on a 5% white wash; active is full paper, medium weight, on a 10% white wash.
- **Active marker:** a 4px by 16px ochre bar with a rounded tip, pinned to the sidebar's left edge beside the active link.
- **Mobile:** below 1024px the same content slides in as a 256px drawer over a 40% ink scrim in 200ms, and links grow to 10px vertical padding. The top bar is sticky with a 1px bottom border.
- **Segmented control:** a 6px-cornered track with 4px segments and medium 14px labels. Two forms exist today: a paper track with a white active segment inside a card, and a white track with an ink active segment on the page. Treat this one as unsettled.

### Score ring
The signature mark of the product. A circular arc on a track of its own color at 15%, with round caps, starting at twelve o'clock. Default is 96px with an 8px stroke; the hero version is 128px with a 10px stroke. The number sits in the center in mono semibold (30px, or 36px in the hero). Below it are an eyebrow label and then the verdict word in 12px medium. The arc fills once over 200ms when it appears and not at all under reduced motion. The arc and number take the tier color; the verdict word in the middle tier uses Deep Marking Ochre. On ink the ring uses the on-ink colors and its label is paper at 60%. With no score, the ring is a 2px dashed circle with a link to create one.

### Ink slab
The Dashboard hero, and the only one in the app. A 16px-cornered block of ink with paper text. The top row holds the date line (mono, uppercase, Marking Ochre on Ink), the Display greeting, a one-line status and an inverted button, with the hero score ring to the right behind a 1px paper divider at 10%. Below, a strip of stat tiles runs 2 across on phones and 4 across from 640px, divided by the same hairlines. Each tile has an 18px icon, a chevron, a large mono figure and an eyebrow label; hover adds a 5% paper wash and nudges the chevron.

### Proportional bars
Bars show a share of a whole and always carry their numbers.
- **Pipeline bar:** 12px tall and fully round. Segments are sized by count, separated by 2px, on a Ruled Line track, and colored by stage. It wipes in from the left once over 450ms. The legend below repeats each stage as dot, label and a 24px mono count.
- **Coverage bar:** 8px tall. Proof Green fill on a 20% brick track, with the label on the left and "4 of 6 · 67%" in mono on the right.
- **Progress bar:** 6 to 10px tall on a Ruled Line track. The fill is ochre and turns green at 100%.

### Application board
- **Column:** Ruled Line at 30%, 12px corners, 10px padding, at least 240px wide. The header is a stage dot, a small title and a mono count. While a card is dragged over it, the column takes an ochre border at 60% and a 5% ochre fill. An empty column shows a dashed "Drop a card here".
- **Card:** a clickable card with 12px padding: title, company caption, score pill, "3d in stage" in mono, a follow-up line, a hairline, and a compact stage select. A dragged card drops to 40% opacity. A card that arrives in a column settles in with a 6px rise over 180ms; nothing animates on first load.
- **Detail drawer:** slides in 24px from the right over 220ms, up to 448px wide, above a 30% ink scrim. A white header holds a 20px Space Grotesk title, the stage pill and a 40px icon button to close. The body is paper, split into sections by hairlines, each under an eyebrow heading.
- **Timeline:** a 1px rule down the left with 8px ash dots; each entry is a 14px line with a caption date.

### Stage suggestion and toast
A card up to 384px wide with the `card-hover` shadow, pinned to the bottom right (bottom center on phones). It asks one question and offers "Not yet" as secondary and the move as primary. After a move, an ink pill with paper text confirms it for three seconds.

### Roadmap timeline
A vertical 2px rail with 32px round nodes. A finished week is a solid ochre node with an ink check. The current week is a white node with a 2px ochre border, a Deep Marking Ochre number and a 4px ochre halo at 15%, and its card takes the emphasized border. Upcoming weeks have a Ruled Line border and an ash number. A completed task row turns to a 30% green border with a 5% green fill and a struck-through ash title.

## Do's and Don'ts

### Do:
- **Do** set every score, count, percentage and day count in IBM Plex Mono with tabular numerals.
- **Do** use Deep Marking Ochre (#8A6218) for any ochre-family text, and keep Marking Ochre for lines, rings, bars and tints.
- **Do** pair every verdict color with a word or a number: the score ring says "Strong", the stage pill says "Offer".
- **Do** mark an application that needs attention with the 3px Correction Brick left edge and a brick reason line under the title.
- **Do** switch to the on-ink colors and paper-at-opacity text on the sidebar and the hero slab.
- **Do** build lighter values from the palette at opacity: 10% behind pills on white, 5% behind banners on paper.
- **Do** design the loading, empty and failure state with every panel: a skeleton in the shape of the content, a dashed empty state with a link to the next step, a bordered brick banner with Retry.
- **Do** keep every control at least 40px tall, including the small-label buttons inside a dense row.
- **Do** label fields with an eyebrow above them or an inline label beside them.

### Don't:
- **Don't** set plain Marking Ochre (#B8842A) as text under 18px. It reaches only 3.3:1 on white.
- **Don't** put readable text on ink below paper at 50%.
- **Don't** fill a surface larger than a progress bar or a 32px node with ochre.
- **Don't** add gradients, purple, sparkle motifs, or glass and blur as decoration. The sticky top bar is the only blur.
- **Don't** use a weight above 600 or below 400, and don't set labels or prose in mono.
- **Don't** add a third shadow, or a shadow in pure black.
- **Don't** reach for Tailwind's default grays or color scales. Every color is one of the palette tokens or white.
- **Don't** signal a state with color alone.
- **Don't** animate on a repeat load or loop anything except the skeleton pulse.
- **Don't** copy the earlier page patterns still present in some pages: 20px page headings without tight tracking, borderless brick strips for errors, a dashed card as an empty state, list rows with no shadow or hover, "Loading…" text in place of a skeleton, and fields labeled only by a placeholder.
