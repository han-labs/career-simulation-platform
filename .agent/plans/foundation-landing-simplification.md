# Foundation Landing Simplification Plan

Status: Implemented  
Owner: Shared frontend foundation with explicit landing-page authorization  
Date: 2026-09-13

## Scope and decision

Lightly simplify the public landing page while retaining CareerSim's approved warm, evidence-led design. Keep the hero, three-step explanation, and starter simulations; remove the redundant track section, repeated closing CTA, and non-essential micro-copy. Replace the minimal global footer with a professional responsive footer containing brand context and primary navigation.

The change does not alter assessment, simulation, guidance behavior, API contracts, scoring, authentication, or data collection. The Forage reference is used only for scannable hierarchy, strong calls to action, restrained section count, and structured footer navigation; no branding or layout is copied.

## Verification

- Frontend lint, unit tests, and production build.
- Responsive inspection at desktop and narrow mobile widths.
- Keyboard-focus and heading/landmark review.
- Existing catalog fallback and primary navigation remain functional.
