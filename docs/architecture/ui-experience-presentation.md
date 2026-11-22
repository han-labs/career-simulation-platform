# HCM-UTE career experience presentation

Scope: the Team Lead's 2026-11-17 visual polish. Existing page order, routes,
public catalogue contracts and Admin Studio's three product areas stay authoritative.

## Shared visual rules

- Keep the existing HCM-UTE blue/navy tokens, sans-serif font, campus imagery and grids.
- Highlight the homepage platform label with warm gold/navy. Use pale blue/navy for
  other experience labels and brighter institutional blue/white for actions and
  active Admin navigation/tabs. Keep selection text/ARIA state in addition to color.
- `hcmute.css` defines `--experience-*` presentation tokens: gold platform label,
  teal introductory metadata, warm-gold intermediate metadata and existing violet
  advanced metadata. Difficulty values still come from the API.
- Keep cards compact; emphasize headings/borders and readable metadata rather than
  adding decorative cards, large shadows, gradients, new fonts or libraries.
- Do not repeat an experience label over each compact catalogue image.
- Preserve keyboard outlines, 44px existing icon-control targets, loading/empty/error
  controls, current responsive breakpoints and reduced-motion behavior.
- Vietnamese product labels say “bài trải nghiệm” / “trải nghiệm nghề nghiệp”.
  English product names, translation lookup keys, URLs, API fields, backend entities,
  stored conversations and technical uses of “mô phỏng” are not renamed.

## Development skill

Project-local UI UX Pro Max: [SKILL.md](../../.agents/skills/ui-ux-pro-max/SKILL.md).
Its [source record](../../.agents/skills/ui-ux-pro-max/SOURCE.md) pins the upstream
commit; the MIT license is included. Its generic palette/font/layout recommendations
do not replace the current product design or authorize runtime dependencies.

## Review boundary

Frontend fixtures establish component presentation only. Real catalogue/auth/AI UAT
requires the existing careersimfresh stack; never describe fixture screenshots as
live backend evidence. Team Lead visual acceptance precedes commit/push/merge.
