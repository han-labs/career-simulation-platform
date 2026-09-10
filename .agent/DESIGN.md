# Career Simulation Platform UI Design System

## 1 Product mood

The product should feel curious, calm, practical, and student-centered. It should invite experimentation without looking childish or presenting career guidance as an authoritative verdict.

The design adapts useful structural cues from education platforms and The Forage: clear discovery, scannable simulation cards, visible duration/difficulty, task progress, and reflection after work. Do not copy their branding, layouts pixel-for-pixel, employer assets, or content.

Avoid hard corporate-blue dashboards, dark page backgrounds, gradients, aggressive contrast, decorative glass effects, parallax, and flashy animation.

## 2 Color tokens

```css
--page-bg: #F5F1E8;
--paper: #FCFBF7;
--card: #FFFFFF;
--ink: #19211F;
--muted: #67716D;
--line: #DCDED8;
--accent: #DF6F49;
--accent-dark: #AA4425;
--sage: #DFEADD;
--sage-ink: #28583B;
--blue: #DCECF2;
--blue-ink: #245F74;
--violet: #E8E1EF;
--violet-ink: #604B78;
--gold: #F1E5BE;
--gold-ink: #735C15;
```

Use color to classify tracks and express state, never as the only carrier of meaning. Text and icons must accompany success, warning, error, active, and fallback states.

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
