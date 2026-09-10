---
version: alpha
name: Forage

description: Design system for Forage (theforage.com), a career empowerment platform connecting students and job seekers with virtual work experience programs from leading companies.

colors:
  # Core brand colors
  primary: "#144c8b"
  primary-light: "#1c6ac3"
  primary-subtle: "#f6fafe"
  primary-100: "#c9def8"
  primary-200: "#76adeb"
  primary-300: "#4d94e5"
  on-primary: "#ffffff"

  # Client/brand palette (theming)
  client-primary: "#0065fc"
  client-primary-light: "#3392ff"
  client-primary-dark: "#004799"
  client-secondary: "#ffcc00"
  client-secondary-light: "#ffd633"
  client-secondary-dark: "#997a00"

  # Secondary / accent
  secondary: "#7950f2"
  secondary-light: "#9775fa"
  secondary-subtle: "#f7f5ff"
  on-secondary: "#ffffff"

  # Neutrals
  surface: "#ffffff"
  surface-dim: "#fbfbfb"
  surface-container-low: "#f7f7f7"
  surface-container: "#efefef"
  surface-container-high: "#e4e4e4"
  on-surface: "#3f3f3f"
  on-surface-subtle: "#737373"
  on-surface-disabled: "#949494"
  outline: "#e4e4e4"
  outline-variant: "#d7d7d7"

  # Semantic — success
  success: "#0a7f21"
  success-subtle: "#fbfefb"
  success-container: "#b2f2bb"
  on-success: "#ffffff"
  on-success-container: "#097129"

  # Semantic — warning
  warning: "#be5700"
  warning-subtle: "#fffdf5"
  warning-container: "#ffec99"
  on-warning-container: "#803a00"

  # Semantic — danger / error
  error: "#cc0000"
  error-subtle: "#fff5f5"
  error-container: "#ffcdcd"
  on-error: "#ffffff"
  on-error-container: "#900000"

  # Semantic — info
  info: "#007195"
  info-subtle: "#f5fdff"
  info-container: "#cbf2fe"
  on-info: "#ffffff"
  on-info-container: "#005b78"

  # Jade / green accent
  jade: "#14a772"
  jade-subtle: "#f6fdfc"
  jade-container: "#e0fbf3"
  on-jade-container: "#00775b"

  black: "#000000"
  white: "#ffffff"

typography:
  headline-display:
    fontFamily: DM Sans
    fontSize: 72px
    fontWeight: "700"
    lineHeight: 1.1
    letterSpacing: -0.025em

  headline-lg:
    fontFamily: DM Sans
    fontSize: 46px
    fontWeight: "700"
    lineHeight: 1.2
    letterSpacing: -0.025em

  headline-md:
    fontFamily: DM Sans
    fontSize: 2.125rem
    fontWeight: "700"
    lineHeight: 1.2

  headline-sm:
    fontFamily: DM Sans
    fontSize: 1.625rem
    fontWeight: "700"
    lineHeight: 1.3

  body-lg:
    fontFamily: DM Sans
    fontSize: 22px
    fontWeight: "400"
    lineHeight: 1.5

  body-md:
    fontFamily: DM Sans
    fontSize: 1rem
    fontWeight: "400"
    lineHeight: 1.5

  body-sm:
    fontFamily: DM Sans
    fontSize: 0.875rem
    fontWeight: "400"
    lineHeight: 1.5

  label-lg:
    fontFamily: DM Sans
    fontSize: 1rem
    fontWeight: "500"
    lineHeight: 1.5

  label-md:
    fontFamily: DM Sans
    fontSize: 0.875rem
    fontWeight: "500"
    lineHeight: 1.5
    letterSpacing: 0.025em

  label-sm:
    fontFamily: DM Sans
    fontSize: 0.75rem
    fontWeight: "500"
    lineHeight: 1.5
    letterSpacing: 0.0625rem

rounded:
  none: 0px
  sm: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 1rem
  2xl: 2rem
  full: 9999px

spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 48px
  gutter: 16px
  margin: 24px

shadows:
  sm: "0 1px 2px 0 rgba(0,0,0,0.05)"
  md: "0 4px 8px -1px rgba(0,0,0,0.08), 0 2px 4px -2px rgba(0,0,0,0.05)"
  lg: "0 10px 24px -4px rgba(0,0,0,0.10)"
  xl: "0 20px 40px -8px rgba(0,0,0,0.12)"

components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label-md}"
    rounded: "{rounded.md}"
    padding: "0.5rem 1rem"

  button-primary-hover:
    backgroundColor: "{colors.primary-light}"
    textColor: "{colors.on-primary}"

  button-secondary:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    typography: "{typography.label-md}"
    rounded: "{rounded.md}"
    padding: "0.5rem 1rem"

  button-secondary-hover:
    backgroundColor: "{colors.surface-container-low}"
    textColor: "{colors.on-surface}"

  button-destructive:
    backgroundColor: "{colors.error}"
    textColor: "{colors.on-error}"
    typography: "{typography.label-md}"
    rounded: "{rounded.md}"
    padding: "0.5rem 1rem"

  button-destructive-hover:
    backgroundColor: "{colors.error-container}"
    textColor: "{colors.on-error-container}"

  card-default:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.lg}"
    padding: "{spacing.lg}"

  input-field:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: "0.5rem 1rem"

  table-header:
    backgroundColor: "{colors.surface-container}"
    textColor: "{colors.on-surface}"
    typography: "{typography.label-md}"
    padding: "0.75rem"

  badge-success:
    backgroundColor: "{colors.success-container}"
    textColor: "{colors.on-success-container}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.full}"
    padding: "0.25rem 0.625rem"

  badge-warning:
    backgroundColor: "{colors.warning-container}"
    textColor: "{colors.on-warning-container}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.full}"
    padding: "0.25rem 0.625rem"

  badge-error:
    backgroundColor: "{colors.error-container}"
    textColor: "{colors.on-error-container}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.full}"
    padding: "0.25rem 0.625rem"

  badge-info:
    backgroundColor: "{colors.info-container}"
    textColor: "{colors.on-info-container}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.full}"
    padding: "0.25rem 0.625rem"

  link-default:
    textColor: "{colors.primary-light}"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: "0.5rem 1rem"

  link-hover:
    backgroundColor: "{colors.surface-container-low}"

  alert-warning:
    backgroundColor: "{colors.warning-subtle}"
    textColor: "{colors.on-warning-container}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: "{spacing.md}"

  alert-error:
    backgroundColor: "{colors.error-subtle}"
    textColor: "{colors.on-error-container}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: "{spacing.md}"

  alert-success:
    backgroundColor: "{colors.success-subtle}"
    textColor: "{colors.on-success-container}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: "{spacing.md}"

  alert-info:
    backgroundColor: "{colors.info-subtle}"
    textColor: "{colors.on-info-container}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: "{spacing.md}"
---

## Overview

Forage is a career empowerment platform built for students and early-career job seekers. Its design system projects trust, accessibility, and professional confidence — anchored by a deep navy blue that signals credibility, paired with clean white surfaces and generous whitespace that keep the experience approachable and uncluttered.

The overall design voice is **clean, professional, and approachable**. The interface leans on a structured, data-rich layout vocabulary drawn from a Tailwind-based utility system, while maintaining a human-centered warmth through soft rounded corners, readable type, and a layered semantic color system for status feedback. Transitions are fast (150ms, ease-in-out) to keep the experience feeling sharp and responsive.

The platform is built to serve a global, diverse audience of students, meaning legibility, contrast discipline, and semantic clarity are non-negotiable at every component level.

## Colors

The Forage palette is organized into five groups: brand, client theming, neutrals, semantic feedback, and accent.

**Brand (Navy)**
The navy scale is the backbone of the brand. {colors.primary} (#144c8b) is the dominant brand color, used for navigation, headings, and key UI chrome. {colors.primary-light} (#1c6ac3) is the interactive link and action color. {colors.primary-subtle} (#f6fafe) provides a near-white tinted surface for brand-adjacent containers.

**Client Theming**
Forage white-labels experiences for partner companies. The `client-primary` and `client-secondary` scales are runtime-swappable tokens that power the branded program UI without affecting the core shell.

**Neutrals**
A gray ramp from {colors.surface} (pure white) through {colors.surface-container-high} (#e4e4e4) handles all surface layering. Text uses {colors.on-surface} (#3f3f3f) for primary content and {colors.on-surface-subtle} (#737373) for secondary/supporting text. Borders and dividers use {colors.outline} (#e4e4e4).

**Semantic Feedback**
A complete four-state semantic system — success (green), warning (amber/orange), error (red), and info (teal) — each with a subtle background, a container, and an on-container text color to support accessible status indicators, alerts, and badges.

**Accent**
{colors.secondary} (#7950f2) is a violet accent used sparingly for highlights, pills, and selection states where the navy brand color would be too heavy.

## Typography

Forage uses a single typeface: **DM Sans**, a low-contrast geometric sans-serif with humanist characteristics that make it highly legible at both display and body scales. It is available on Google Fonts. The font is self-hosted from Forage's CDN in Regular (400), Medium (500), and Bold (700) weights, along with matching italic cuts.

The type scale is built on a modular ratio defined by the CSS custom properties `--text-*`, spanning from 0.625rem (2xs) to 4.5rem (7xl). The design system maps these to semantic roles:

- **headline-display / headline-lg:** Used for hero sections and top-of-page headings. Bold weight (700) with tight letter-spacing (-0.025em) to create authority and visual punch at large sizes.
- **headline-md / headline-sm:** Section and subsection headings. Bold weight maintains hierarchy without requiring size extremes.
- **body-lg:** Primary reading text in content-heavy sections. 22px at weight 400 with a 1.5 line-height ensures comfortable long-form legibility.
- **body-md / body-sm:** UI prose, card descriptions, and supporting text. Standard 1rem / 0.875rem at regular weight.
- **label-md / label-sm:** Buttons, table headers, tags, and interactive controls. Medium weight (500) and optional letter-spacing keep these distinct from body text at small sizes.

No proprietary font substitution is required — DM Sans is freely available via Google Fonts.

## Layout & Spacing

The layout system is derived from a base spacing unit of 4px (`--spacing: 0.25rem`), creating an 8px-aligned rhythm for most component internals. Container widths span from `--container-xs` (20rem) to `--container-7xl` (80rem), with key breakpoints at `--breakpoint-lg` (64rem) and `--breakpoint-xl` (80rem).

- **xs (4px):** Icon gaps, tight inline spacing.
- **sm (8px):** Internal component padding, compact list items.
- **md (16px):** Standard card padding, form field insets, grid gutters.
- **lg (24px):** Section-level padding, card outer margins.
- **xl (48px):** Page section vertical separation, hero padding.
- **gutter (16px):** Column gutter in multi-column grids.
- **margin (24px):** Page-edge safe margin on constrained viewports.

Content is centered within a max-width container. The layout philosophy prioritizes readability: content lines should not exceed comfortable measure widths, and whitespace between sections should always feel generous to reduce cognitive load for users navigating program catalogs and task flows.

## Elevation & Depth

Forage relies primarily on **tonal layering** rather than heavy drop shadows to create depth. The neutral surface ramp — from {colors.surface} (white) through {colors.surface-container-high} — creates visible but non-distracting hierarchy between page backgrounds, cards, table rows, and overlays.

Drop shadows are used selectively:

- **sm:** Barely perceptible lift for interactive inline elements.
- **md:** Standard card and dropdown shadow — soft blur, low opacity.
- **lg:** Modal dialogs and floating panels — wider spread, still subdued.
- **xl:** Reserved for full-screen overlays or very prominent floating elements.

All shadows use neutral rgba black with low opacity, consistent with the clean, minimal visual voice. No colored shadows are applied. On hover, interactive cards should transition to the next shadow level (e.g., sm to md) over 150ms ease-in-out.

## Shapes

The shape language uses **moderately rounded corners**, balancing a professional data-product feel with the approachability appropriate for a student-facing platform.

- **none (0px):** Rarely used; reserved for full-width dividers and flush-edge table cells.
- **sm (0.25rem / 4px):** Subtle rounding for tags and inline code chips.
- **md (0.375rem / 6px):** Default for buttons, input fields, and link pills — the most common radius in the UI.
- **lg (0.5rem / 8px):** Cards and panel containers.
- **xl (1rem / 16px):** Prominent feature cards, modal containers.
- **2xl (2rem / 32px):** Hero banners and large illustration containers.
- **full (9999px):** Status badges, count indicators, and avatar frames.

Corner radius should be applied consistently within a component family. Mixing radii within a single container is discouraged.

## Components

### Buttons

The primary button uses {colors.primary} (#144c8b) as its background with {colors.on-primary} (white) text, rounded at {rounded.md} (6px). On hover, the background shifts to {colors.primary-light} (#1c6ac3) over a 150ms ease-in-out transition. The secondary button is a bordered ghost style using {colors.surface} background and {colors.on-surface} text, gaining {colors.surface-container-low} on hover. The destructive button uses {colors.error} and shifts to the error container on hover.

### Cards

Cards sit on {colors.surface} (white) with {rounded.lg} corners and {spacing.lg} padding. They are elevated one step above the page background via tonal contrast rather than shadow, though {shadows.md} may be applied for interactive program cards that lift on hover.

### Input Fields

Form inputs use a transparent-to-white background, {colors.on-surface} text, {rounded.md} radius, and a 1px border using {colors.outline}. The focus ring uses the `--ring` variable resolved to navy (#144c8b-family). Disabled inputs use {colors.on-surface-disabled} text.

### Badges & Status Indicators

A full set of semantic badges is provided — success, warning, error, and info — each using their respective subtle container and on-container token pairing for accessible contrast. All badges use {rounded.full} for pill presentation and {typography.label-sm} for legibility.

### Alerts

Alerts mirror the badge semantic structure but at block level, using the `-subtle` background token and the `on-*-container` text color for each state. They use {rounded.md} and {spacing.md} padding.

### Tables

Table headers use {colors.surface-container} (#efefef) background with {typography.label-md} (semibold) text and a 0.75rem cell padding. Row borders use {colors.outline-variant} (#d7d7d7). Hover rows gain {colors.surface-container-low} as a background highlight.

### Links

Inline links use {colors.primary-light} (#1c6ac3), matching the `--color-link` token. Navigation link pills use {rounded.md}, a 1px {colors.outline} border, and gain {colors.surface-container-low} on hover — consistent with the secondary button style.

## Do's and Don'ts

**Do's**
- Use {colors.primary} as the default action color for all primary CTAs and active navigation states to maintain consistent brand identity.
- Use the semantic color tokens ({colors.success}, {colors.warning}, {colors.error}, {colors.info}) and their container/on-container pairs for all status feedback — never use raw hex values inline.
- Apply {rounded.full} exclusively to badges, avatars, and pill-style indicators; use {rounded.md} for all interactive buttons.
- Maintain the 4px/8px spacing rhythm: always derive spacing from the {spacing.*} token scale rather than arbitrary pixel values.
- Use {typography.label-md} at weight 500 for all button and interactive control labels to keep them visually distinct from body prose.
- Pair the `client-primary` and `client-secondary` token families for all partner-branded program pages, leaving the core navy {colors.primary} shell unchanged.

**Don'ts**
- Do not place {colors.on-surface-subtle} text on {colors.surface-container} backgrounds without verifying contrast — this pairing can fail WCAG AA at small text sizes.
- Do not use {colors.secondary} (violet) as a primary action color; it is reserved for accents, selection highlights, and non-critical decorative elements.
- Do not apply {shadows.xl} to inline or card-level elements — reserve it for modal overlays and page-level floating panels only.
- Do not use font weights outside the defined set (400 regular, 500 medium, 700 bold) — no thin, light, extralight, or black weights appear in the deployed font stack.
- Do not reduce button border-radius below {rounded.md} for interactive controls — sharper corners conflict with the platform's approachable visual tone.
- Do not use {colors.client-primary} or {colors.client-secondary} outside of client-branded program contexts — these tokens are runtime-injected and may change per partner, making them unsuitable for core UI chrome.
