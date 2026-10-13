# Website localization verification — 2026-10-13

Baseline commit: a69429088d793a8b9ec080b514339eac93c3d274; evidence is for the dirty working tree, not a new committed revision. Existing unrelated changes preserved.

Trace: `plans/US-01-03-site-localization.md`, current explicit project-lead request. Architecture: `docs/architecture/site-localization.md`; additive contract: `docs/api/site-localization.md`.

Implemented:
- Shared English/Vietnamese preference in every current route's header, persists across reloads and synchronizes between tabs; works in memory if storage fails.
- Correct Vietnamese school identity: Đại học Công nghệ Kỹ thuật TP. HCM.
- All current static page/accessible labels, landing cards, assessment prompts/scales/results, simulation prompts/options, current dashboard metadata and Syn controls.
- Backend-only post-evaluation Vietnamese explanations for the fifteen current tasks. Original English, IDs, scoring, persisted evaluations and public task contract unchanged.
- Syn future reply language follows website selection by default; explicit override retained. Historical/private/generated prose and source quotations remain original.
- Responsive menu adjusted for longer translated labels; 320px landing and dashboard show no horizontal overflow. Assessment demo controls wrap and result grid shrinks safely.

Gates:
- Frontend: npm run check, 56 tests passing; lint/build passed.
- Backend: mvnw.cmd spotless:apply verify, 149 discovered, 145 executed, 4 opt-in skips, no failure/error; formatting/architecture passed.
- Compose validation and git diff --check passed.
- Final Docker rebuild of backend/frontend and gateway restart passed; existing PostgreSQL volume retained. All services running, backend/postgres healthy; final gateway health UP.
- Browser reloaded final served bundle index-BkZdfZ4t.js and retained Vietnamese; header selector, exact school name and current runtime catalog visible.
- Final frontend image: 948adb3b8cdbca70f8aba67c0fac94df234a4d28020eedba073573cdb2ce3c21; backend image: eebe57ccab96a105f05f0a480901521313ddcc51bc40e5d4136ceab94b94e98b.
- Final gateway public tasks: 3 tasks, no explanation field; assessment endpoint: 42 prompts. New evaluated explanations absent from public frontend dictionary/bundle. Evaluated POST response covered by controller/mapper tests, not by adding a persisted live demo attempt.
- Screenshot: .codex-tmp/site-vietnamese-2026-10-13.jpg. Temporary viewport restored to normal (916px observed); final desktop document width 901px, no horizontal overflow.

Browser evidence:
- Language changed in-place on actual running app, document title changed, reload retained Vietnamese.
- Catalog showed five localized runtime simulations; data simulation detail listed its three localized tasks.
- Selected option b in data task remained checked with same ID when switching to English then back. No submission or persisted demo attempt created.
- Assessment intro, synthetic dashboard statuses/history/dates/plan, Syn welcome/controls/quick actions in Vietnamese.
- Syn conversation preservation/language request tested with mocks; no live model call or public-corpus change made.

Known boundaries: future content retains original wording; existing messages and external passages are not rewritten; original local three-task mock/temporary identity and landing track link mismatches are not repaired by this change.
