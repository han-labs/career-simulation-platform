# Website EN/VI presentation

Scope: current landing, assessment, simulation catalog/detail/attempt/result, dashboard and Syn controls. Requested on 2026-10-13. Trace: `plans/US-01-03-site-localization.md`.

Root `I18nProvider` -> validated `careersim_ui_language_v1` preference -> React context -> `useI18n()` -> view-only `t(sourceText)`. `LanguageSwitcher` is part of `AppShell`, remains visible when the responsive menu is closed. Switching updates document language/title/description and existing components in place; it does not remount the router, change an attempt ID, send a submission, or refetch data. Preference storage is best-effort and synchronized between tabs.

Current English content has reviewed Vietnamese presentation entries. Question and option IDs, scores, technical SQL/HTTP examples, links, user-entered text and quoted sources remain unchanged. Future content without a reviewed translation retains original wording.

Post-submission explanation flow: controller -> existing deterministic service/evaluation/persistence -> response-only `SimulationResultLocaleMapper` -> optional `TaskOutcomeDto.explanationVi` -> result UI chooses English/Vietnamese field. Public `SimulationTaskDto` stays free of evaluation explanations/keys. The optional translation is omitted from the service's persisted outcome representation; scoring and transactions are unchanged. Do not add evaluated answer explanations to the public frontend dictionary. Existing three-task local backend mock remains a pre-existing demo limitation, not a newly secured production scorer.

Syn: UI controls follow website preference. Reply-language AUTO means selected website language; an explicit EN/VI override remains possible. New requests use the existing server responseLanguage field. Existing messages, model prose, external resource titles, passages, plan drafts and provider HTML are not rewritten or remotely translated. Application-owned welcome/error/save text is localized separately. Transcript exports localize headings/provenance without changing source claims.

No new translation API, dependencies, identity mechanism, schema migration, RAG ingestion, model generation or score rule is introduced. Production authentication remains existing open work. Translation review by the feature/content owners remains recommended when new material is published.

## Current shared shell and Agent presentation — 2026-10-28

The historical Syn paragraph above does not restore its retired runtime. Current
Student Agent and Admin Preview share `AgentActivity` for accessible pending feedback;
the indicator honors reduced motion. The Student launcher toggles the same mounted
popup using visibility/inertness and a 200ms transition. Closing does not cancel or
resubmit an execution. Chat headers use the existing blue palette.

`simulationMessages.js` covers reviewed public overviews, objectives and task copy
from the nine imported simulations, including their published/pinned versions.
`workspaceMessages.js` covers system lifecycle labels, skills, deliverable and curated
reference-link labels. `localized()` applies reviewed display copy after explicit
row translations. Unknown content, authored Agent/KB names and instructions, stored
messages, evidence quotations, code, identifiers, request values and URLs stay original.
No new translation service, provider call, backend or persisted-content change occurs.
Unresolved source export references such as `$65` are not fabricated into instructions.

The shared header uses logo/navigation/actions columns, centered desktop navigation
and one breakpoint at 1100px for both languages. Narrow screens retain a right-aligned
actions row and accessible menu. Footer spacing and type are compact at each breakpoint.
Simulation filters/choice controls translate labels while retaining original option
values. Reference matching continues to use original API metadata, not translated text.
