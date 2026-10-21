<!-- documentation-sync-2026-10-22 -->
> **Current scope, 2026-10-22:** this file defines CareerSim UI/UX (sections 1–8). Use [current UC/US](../docs/requirements/README.md), [team assignments](../docs/TEAM_ASSIGNMENTS.md), [architecture](../docs/architecture/README.md) and [API contracts](../docs/api/README.md) for business/technical work.
>
> Dated technical/verification checkpoints below are preserved historical notes, not a second architecture or progress authority. [TASKS](TASKS.md) owns current progress. The Forage reference is inspiration only, not CareerSim's binding palette or licensed task content. Do not infer completion, plan-editing support or live AI acceptance from UI presentation; see [alignment](../docs/requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md).

<!-- preserved-content-start -->
# Career Simulation Platform UI Design System

Public product shorthand: **CareerSim**.

## 1 Product mood

The product should feel curious, calm, practical, and student-centered. It should invite experimentation without looking childish or presenting career guidance as an authoritative verdict.

The design adapts useful structural cues from education platforms and The Forage: clear discovery, scannable simulation cards, visible duration/difficulty, task progress, and reflection after work. Do not copy their branding, layouts pixel-for-pixel, employer assets, or content.

Avoid hard corporate-blue dashboards, dark page backgrounds, gradients, aggressive contrast, decorative glass effects, parallax, and flashy animation.

## 2 Color tokens

```css
--page-bg: #F4F2EC;
--paper: #FBFAF7;
--card: #FFFFFF;
--ink: #142421;
--muted: #64726E;
--line: #D7DDD9;
--accent: #D96843;
--accent-dark: #9F4026;
--tech: #1F6679;
--tech-deep: #174D5C;
--tech-soft: #DCEEF2;
--sage: #DFECE3;
--sage-ink: #28583B;
--blue: #DCECF3;
--blue-ink: #1C6178;
--violet: #E8E1EF;
--violet-ink: #604B78;
--gold: #F1E5BE;
--gold-ink: #735C15;
```

Warm neutral surfaces keep the product welcoming; restrained teal/blue accents communicate the technology focus without turning the experience into a corporate dashboard. Use color to classify tracks and express state, never as the only carrier of meaning. Text and icons must accompany success, warning, error, active, and fallback states.

## 3 Typography

- Display headings: Georgia or an approved readable serif, 40-88 px depending on viewport.
- Interface/body: Inter, Aptos, Segoe UI, or system sans-serif.
- Page/section hierarchy must be visible through size, weight, spacing, and language.
- Body text: 15-18 px, line height 1.6-1.75.
- Labels: 11-12 px, uppercase only for short metadata such as difficulty or category.
- Never use all-caps headings or overly long centered body copy.

## 4 Layout and spacing

Use a 4/8 px base scale: `4, 8, 12, 16, 24, 32, 48, 64, 96`.

- Desktop content width: approximately 1160 px.
- Page gutters: 20 px minimum desktop, 14 px minimum small mobile.
- Section spacing: 72-96 px.
- Card radius: 16-18 px; hero/large panels up to 28 px.
- Prefer borders and whitespace over heavy shadows.
- Responsive layouts must work at 320 px without horizontal scroll.

## 5 Components

### Buttons

- Primary: dark ink background, white label, 12 px radius.
- Secondary: transparent with subtle neutral border.
- Use specific labels: `Start assessment`, `Resume simulation`, `Submit answers`, `View feedback`.
- Disabled state must explain why when the reason is not obvious.
- Preserve a visible `:focus-visible` outline; never remove focus without an equivalent.

### Cards

- White/paper surface, 16-18 px radius, 20-28 px padding.
- Metadata appears before title when it helps scanning.
- Hover lift is at most 2-3 px and must not be required to discover content.
- Cards that navigate must have one clear accessible link target.

### Forms and tasks

- Label above each field/control group.
- Put validation near the field and provide a summary for multi-field submission errors.
- Preserve user work on recoverable failures.
- Do not use color-only correct/incorrect indicators.
- Confirm final simulation submission because it may lock answers.

### Progress and scores

- Show current step, total steps, and save state.
- Every chart requires visible numeric/text equivalents.
- Keep the six RIASEC dimensions ordered consistently.
- Objective score and AI/fallback guidance must be visually distinct.

### AI and fallback disclosure

- Label report source with plain language: `AI-assisted explanation` or `Standard guidance`.
- Do not style AI output as more authoritative than objective evidence.
- Show a calm maintenance message when fallback is used; the result itself remains usable.
- Never expose raw system prompts, provider errors, or model credentials.

## 6 Interaction rules

Allowed: small hover lift, instant active state, progress transitions, skeletons, and short status changes.

Avoid: page fades that delay work, scroll-triggered effects, auto-advancing questions, celebratory motion after sensitive results, or animation without reduced-motion handling.

## 7 Page-specific guidance

### Landing page

- Lead with exploration and student agency.
- Explain the three-step loop: assess, simulate, reflect.
- Show a small simulation catalog with duration, difficulty, track, and clear action.
- Do not lead with AI; AI is an interpretation layer, not the product's only value.

### Assessment

- One coherent question group per step.
- Autosave/resume state is visible.
- Result explains all dimensions and uncertainty.

### Simulation

- Separate instructions, task workspace, evidence/resources, navigation, and save/submit states.
- Do not reveal evaluation configuration before completion.

### Guidance report

- Recommended order: objective evidence, strengths, difficulties, limitations, possible next actions, source/provenance.
- Never state that a student “must” pursue a career based on these results.

## 8 Accessibility gate

- Target WCAG 2.2 AA contrast and interaction behavior.
- Keyboard access, semantic landmarks, correct heading order, form labels, error association, visible focus, and reduced motion are required.
- Icon-only controls require an accessible name.
- Touch targets should be at least 44 by 44 CSS pixels where practical.

## Approved Target v1.2 P1/P2 checkpoint — 2026-10-19

The approved Requirements Baseline v1/DR1–DR2 and Target v1.2 supersede earlier
Core diagrams. Identity uses JWT HttpOnly/Secure/Lax cookies with CSRF, explicit
issuer/subject OAuth linking, per-credential logout and current authority checks.
Authentication is not persisted in HttpSession; CSRF/OAuth handshake may use it.
New scenario versions use ordered FINAL + explicit self-review progression; old
published versions retain their original policy/hash. Zero quiz score is valid
completion evidence, and practical work requires no automatic grading.
Physical additions are V19/V20; immutable V1–V18 and normalized BIGINT Core storage
remain. See `../docs/architecture/p1-p2-security-core.md` and the implementation
report for tested contracts and release limitations. No Agent Platform is added.

## Approved Target v1.2 P3/P4 checkpoint — 2026-10-20

KB source upload is private staging; parse/embed and provider calls occur outside
DB transactions. Complete chunks/vectors + READY job commit atomically; explicit
publish switches the active pointer. Configurations and KB grants seal on save;
publish requires a successful preview of the exact saved hash. KB_TEST and
ADMIN_PREVIEW never create a Student session or read private Student evidence.
Execution admission/cancellation/current-source checks surround bounded native
calls; provider protocol stays private, JSON/citations are validated before
publication. Reuse Syn transport/embedding/usage/pricing and V13 vector768 while
fencing scoped KB assets out of legacy retrieval. Session/history tables in V21
are inert FK targets for future P5, not a completed chat feature. See
`../docs/architecture/agent-platform-p3-p4.md` and the API contract.


## P5/P6 implementation checkpoint — 2026-10-20

Approved Student Agent + Reports/Plans/Usage code is integrated on
`feat/student-agent-and-ai-reporting` from main `1b11b593e0bc0cbc613eedfbfb7ba5307f89e12a`.
Existing P1–P4/Core/Syn and V1–V21 remain preserved. Additive V22 adds source lineage,
durable events/reports and exact-source plan-confirmation integrity; no legacy backfill.
Student/Admin/KB hosts share BoundedNativeExecution with native provider adapters;
current owner/assignment/consent/source/epoch checks govern dispatch, commit and replay.
No autonomous business writes or deferred Enterprise/private-upload features.

Final local gates: backend clean verify310 tests (305 pass,0 fail/error,5 skip),
frontend97 pass with lint/build in default and enabled configurations; isolated
fresh/upgrade/checksum/backup/gateway checks pass. Browser13 smoke assertions used
real UI/Nginx/PG and mocked providers.20 actual deterministic schema/evidence cases
are separate from live-model quality. AC-A09 and full AC-A17 remain NOT VERIFIED;
real Google OAuth, approved live-model/human20-case review and release/course delivery
are pending. No commit, push, merge, valuable DB migration or deployment.

See `.agent/reports/p5-p6-completion.vi.md`, verification/preservation manifests,
`docs/api/student-agent-p5-p6.md`, `docs/architecture/student-agent-p5-p6.md` and
`docs/ai/AI_USAGE_P5_P6.md`. Original user content above is preserved byte-for-byte.

<!-- preserved-content-end -->
