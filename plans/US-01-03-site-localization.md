# EN/VI website localization

Status: Implemented and technically verified; content-owner wording review recommended
Owner: Shared frontend change requested by project lead; US-01/02/03 owners retain business ownership
Date: 2026-10-13

## 1 Scope
- Foundation item across US-01, US-02, US-03: English/Vietnamese UI.
- Actor: student/public visitor. Source: current user request, current implemented routes.
- Acceptance: one visible keyboard-accessible language selector; persistent EN/VI preference; translated landing, assessment, simulation, dashboard and Syn controls; correct Vietnamese school name "Đại học Công nghệ Kỹ thuật TP. HCM"; language changes preserve answers, route and chat.
- Exclusions: changing scoring, auth, schema, publishing RAG corpus, translating user messages/provider quotations or external web pages; automatic translation of future unreviewed catalog content.
- Explicit request authorizes coordinated presentation-only edits across frontend owners.

## 2 Flow inventory
### Main success flow
1. Root provider reads validated local preference (English default).
2. Selector updates React context, document language/title and preference.
3. Components render localized labels and reviewed current content without refetching or remounting workflows.
4. New Syn requests use selected language unless the student explicitly selects a reply-language override.
### Alternate flows
1. Switch while answering: identifiers, answer selections and scores unchanged.
2. Existing conversation: original messages and source evidence stay intact; controls change.
### Exception and fallback flows
1. Unavailable storage: language still works in memory.
2. Invalid stored language: default English.
3. Unknown future server content: preserve original source text, never invent translated evidence.

## 3 Responsibility design
| Responsibility | Component | Reason |
|---|---|---|
| Preference and React notification | I18nProvider/useI18n | Shared state without changing feature state |
| Reviewed translations/formatting | locale dictionary/translator | Presentation only, stable identifiers |
| Selection | LanguageSwitcher in AppShell | Available on all routes and mobile |
| Scoring and authorization | Existing backend | Remains authoritative |

## 4 Change surface
- Frontend: root, shared i18n, shell, all current page/component presentation.
- Backend: post-evaluation response mapper adds optional explanationVi to evaluated outcomes only; existing explanation and scoring remain unchanged. Public task DTO, services/domain/DAO/migrations/configuration unchanged.
- Documentation: this plan, verification and AI assistance log.

## 5 API contract
POST /v1/simulations/{slug}/attempts: additive optional outcome.explanationVi for reviewed evaluated text. Existing Syn responseLanguage EN/VI reused; objective request IDs, answer values and English fields unchanged. No localized solutions shipped in the frontend for additional tasks.

## 6 Security privacy and AI
- No new identity/access rules, external translation API, personal data upload or secrets.
- Model output/citations remain original validated content; UI preference affects future requests only.
- Existing AI fallback, timeout, ownership and score boundaries unchanged.
- Preference stores only language; safe storage exceptions; no private transcript persistence added.

## 7 Tests and evidence
- Translation coverage for current static UI and seeded assessment/task content; English fallback for future content.
- Preference validation, persistence failure, document metadata, switch preserving selected answer.
- Syn language request and raw-message/source preservation.
- Existing frontend/backend gates, Compose config, gateway health, browser language/navigation/mobile smoke.
- Commands: npm run check; backend mvnw.cmd verify; docker compose config --quiet; frontend rebuild.

## 8 Risks and decisions
| Risk | Decision |
|---|---|
| Human review of wording | Vietnamese reviewed against original; team content owner review still recommended |
| Third-party source quotations | Do not auto-translate/rewrite quotations or provider HTML |
| Future new content | English source fallback until reviewed translation added |
| Default locale | English retained for existing experience; user may persist Vietnamese |
| Backend state | Never reset attempt/chat on locale change |
