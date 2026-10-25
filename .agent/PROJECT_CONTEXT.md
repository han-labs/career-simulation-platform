<!-- documentation-sync-2026-10-22 -->
# CareerSim current project context

Current documentation synchronization: 2026-10-22, checked against `origin/main@5486f9f4a3a56519c396bcebffd49fe0ff4776e2`. Local checkout remains `6a3e989`; do not discard its existing changes. P1–P6/V22 are already present, not a new implementation request.

## Sources with separate authority

- **Catalogue:** [10 report-selected US](../docs/requirements/user-stories/REPORT_SELECTED_10_US.md), [16 UC](../docs/requirements/use-cases/README.md), from the current report and CORRECTED handoff.
- **Business rules:** [frozen Baseline v1 + DR1–DR2](../docs/requirements/CareerSim_Requirements_Baseline_v1_Chat2_Handoff.md), unchanged. Its 15 US IDs differ from current report IDs.
- **Work allocation:** [TEAM_ASSIGNMENTS](../docs/TEAM_ASSIGNMENTS.md), confirmed by the project lead; [TASKS](TASKS.md) owns current progress.
- **As-built:** [architecture index](../docs/architecture/README.md), [P1/P2](../docs/architecture/p1-p2-security-core.md), [P3/P4 API](../docs/api/agent-platform-p3-p4.md), [P5/P6 API](../docs/api/student-agent-p5-p6.md), source/tests and recorded evidence.
- **Conflicts and provenance:** [report/code alignment](../docs/requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md). A catalogue import is not business-rule approval or live acceptance.

## Product and current boundary

CareerSim supports student IT-career exploration. Student, Enterprise Representative and Administrator use a Spring Boot modular monolith, Java 17, React/Vite **JavaScript/JSX**, PostgreSQL/pgvector, Flyway and a backend-owned Gemini/OpenAI adapter. Logical Target v1.2 does not replace physical Core BIGINT/normalized storage.

Assessment is optional. Results stay separate and versioned. Enrollment pins a ScenarioVersion. Valid FINAL submissions determine completion independently of quiz pass score; explicit self-review unlocks the next task. Model answers remain owner/release authorized. Skills are practiced, not certified proficiency.

Assigned Student Agents use pinned configurations, current consent, scoped KB/read-only tools, durable history/SSE, validated explicit InsightReports and exact-source/hash plan confirmation. Admin Preview and KB Test have no fake Student session. Admin usage aggregates exclude private transcripts/reports. Core remains usable without AI.

**Selected ownership, updated by Team Lead 2026-10-25:** Person 1 UC-01–03 / US-01–03; Person 2 UC-04/05/06 / US-04/05/06/07; Person 3 (project lead) UC-07/08/09 / US-08/09/10. The current branch implements UC-07 only. Supplementary UC-10–16 are not new work assignments; UC-14 recruitment invitations remain deferred. Do not infer real names/Git aliases from old ownership or commit counts.

Keep UC-05 completion/evaluation, UC-04 recommendations, UC-06 plan editing and UC-08 naming discrepancies visible. AC-A09/full AC-A17, real Google OAuth, human/live quality review, deployment/NFR/UAT and backup-retention decisions remain separately evidenced gates. Never invent a 30/90-day Student Agent retention policy.

## Historical context

The original content below is retained byte-for-byte, including pre-existing user checkpoints. Statements such as “auth pending”, legacy 3-US scope, Syn TTLs and earlier package numbering describe their dated increments only; they are not the current task source.

<details>
<summary>Historical content preserved unchanged</summary>

<!-- preserved-content-start -->
# Career Simulation Platform Project Context

### Website EN/VI checkpoint — 2026-10-13

Current public/student routes now share an accessible, persistent English/Vietnamese UI preference. Vietnamese institutional identity is "Đại học Công nghệ Kỹ thuật TP. HCM". Static UI, current 42 questions and five simulations are localized without changing identifiers/scoring. Evaluated explanationVi is a response-only server projection; new answer explanations are not shipped in frontend dictionaries. Syn future reply language follows website preference by default with explicit override; original conversations/source quotations remain untouched. Frontend 56 tests, backend 149 discovered / 145 executed / 4 opt-in skips, Docker/gateway and browser smoke passed. See `reports/site-localization-verification.md` and `../docs/architecture/site-localization.md`; future content requires reviewed translations. Production identity and existing demo limitations remain open.

## 1 Project identity

Project name: **Career Simulation Platform for Students**  
Public product shorthand: **CareerSim**
Repository folder: **career-simulation-platform**  
Course context: **New Technologies in Software Engineering / OOSE-style engineering evidence**  
Product type: **Experiential IT career exploration web platform**

The platform addresses the gap between reading about a career and experiencing representative work. Students assess interest signals, perform short tasks, receive objective results, and reflect with AI-assisted or deterministic guidance. The platform supports exploration; it does not decide a student's career.

## 2 Actors

### Student

- Completes the RIASEC assessment.
- Browses and performs published IT simulations.
- Reviews objective task results and evidence.
- Receives personalized guidance or deterministic fallback.
- Controls the final interpretation and next step.

### Enterprise or content provider

- Creates, edits, publishes, archives, and monitors owned simulations.
- Provides reviewed task content and deterministic evaluation rules.
- Does not access unrelated student-private data.

### Administrator

- Manages account/platform operations approved by later requirements.
- Does not silently change assessment scores or simulation outcomes.

## 3 Essential user stories

- **US-01:** Complete a RIASEC-based assessment and receive several IT directions to explore.
- **US-02:** Complete representative tasks in a career simulation to experience the work.
- **US-03:** Explore assessment and simulation evidence with Syn and form possible next steps without surrendering the final decision.

Detailed acceptance baselines live in `../docs/requirements/user-stories.md`.

## 4 MVP boundary

Included:

- authentication, roles, status, profiles, and AI consent;
- deterministic RIASEC scoring;
- IT simulations for frontend, backend, data analysis, software testing, and cybersecurity;
- deterministic task evaluation using approved rules, answer keys, or test cases;
- a concise student Dashboard and Syn, a bounded evidence-grounded exploration agent with session memory, approved tools, and validated structured output;
- rule/template-based fallback;
- enterprise simulation management;
- PostgreSQL persistence, REST APIs, React UI, Docker Compose, Nginx, and Ubuntu VM deployment evidence.

Excluded:

- VR/AR and fully 3D environments;
- multiplayer simulations;
- direct recruitment/job matching;
- a native mobile application;
- training a proprietary LLM;
- complete offline operation;
- AI as an objective grader.

## 5 Business rules

- BR-01: RIASEC scores are deterministic and auditable.
- BR-02: Simulation scores are deterministic and auditable.
- BR-03: AI may interpret evidence but cannot create or change objective scores.
- BR-04: Recommendations are possible exploration directions, not decisions or guarantees.
- BR-05: Only published simulations appear in the student catalog.
- BR-06: Public task data cannot reveal evaluation rules or answer keys.
- BR-07: An attempt records its student, simulation version/context, timestamps, submissions, and evaluation.
- BR-08: Completed evaluation and guidance are persisted separately.
- BR-09: AI context contains only authorized, relevant evidence and never database credentials or unrestricted records.
- BR-10: AI output must match the approved schema before display or persistence.
- BR-11: Timeout, provider failure, unsafe/malformed output, or unavailable AI triggers deterministic fallback.
- BR-12: Fallback must preserve the complete non-AI workflow and identify its source transparently.
- BR-13: Student-private results are visible only to the student and explicitly authorized roles.
- BR-14: Enterprise users can mutate only content they own or are authorized to manage.
- BR-15: Account status and role checks are server-side; the frontend is not a security boundary.
- BR-16: Source and license/provenance are required for externally derived simulation content or datasets.
- BR-17: No real student data is used as demo/seed data.
- BR-18: Syn session memory stores only a bounded structured summary and can be cleared by its owning student.
- BR-19: Syn may execute only backend-approved read tools; provider output cannot select SQL, change scores, or persist a plan.

## 6 Architecture decisions

- Modular monolith, not microservices, for the eight-week MVP and small team.
- React + Vite frontend.
- Java 17 + Spring Boot backend.
- PostgreSQL with Flyway.
- Nginx and Docker Compose on an Ubuntu VM.
- External hosted LLM behind a backend adapter; provider/model remains configurable.
- Deterministic fallback is the immediate recovery path; a local LLM is future scope.

## 7 Minimum demonstrable vertical slice

The student opens **Backend API Triage**, completes three tasks, and submits answers. The backend validates the request, loads task definitions, scores without AI, persists the attempt/result, and returns objective results to the Dashboard. When requested, Syn receives minimized evidence through approved backend tools and returns a validated explanation or plan draft. If AI fails, the student still receives objective results, deterministic result cards, and Standard guidance.

## 8 External product reference and academic data use

The Forage is an external product reference for the non-commercial course project. The team may collect permitted, publicly accessible factual metadata about IT-related simulations for catalog research, such as the source URL, title, publisher attribution, track/category, public difficulty, duration, task count, and skill labels. Collection must follow the current source terms, `robots.txt`, technical controls, and a conservative rate limit; it must stop when permission cannot be verified.

This approval does not extend to authenticated content, employer task descriptions, task files, model answers, certificates, personal data, brand assets, screenshots, or verbatim republication. CareerSim publishes original or separately licensed simulation tasks. Every external record requires provenance and human review under BR-16. See `../docs/data/simulation-content-policy.md`.

## 9 Current implementation checkpoint

The repository contains a working public catalog, preliminary US-01 assessment and US-02 simulation backends, PostgreSQL/Flyway, Nginx/Docker, CI, and shared agent rules. The synthetic V8 demo slice provides 42 assessment answers/scores, three original tasks for each of five published simulations, and evaluated evidence that can be replaced or extended through normal student actions. US-03 has a concise Dashboard, a global three-mode Syn widget, protected evidence aggregation, bounded 30-day structured session memory, typed intent/tools, a consent-gated server-planned generation flow, optional feature-flagged two-pass Gemini tool calling, reviewed path/resource retrieval, new-student onboarding, English/Vietnamese replies, adaptive response depth, contextual follow-up, strict structured output, and deterministic bilingual Standard fallback through the retained static policy. The model can receive only server-approved read-tool results, cannot exceed two passes when native planning is enabled, and cannot access SQL or persist plans. Provenance audit and confirmed-plan persistence remain server-owned. The conversation anchors a submitted question, progressively reveals only validated text, and never forces the reader to the bottom. Runtime authentication/login and enterprise management remain implementation work. An opt-in, localhost-only demo principal can exercise US-01 to US-03 with the seeded synthetic student during internal testing; it is disabled by default and is not a production identity mechanism.


### Syn P1/P2 checkpoint — 2026-09-28

Approved implementation now includes strict shared output checks, bounded/cancellable HTTP, short snapshot/begin/finalize transactions, durable idempotent turns, ownership/consent rechecks, clear/expiry races, direct older-evidence lookup, and tab-only transcript. A/S references identify authorized completed attempts; K references identify published knowledge chunks and versions. After preserving the assessment migrations already applied at V10–V11, V12–V14 add Syn lifecycle, pgvector knowledge, and trace metadata. Retrieval uses pinned 768-dimensional Gemini embeddings plus PostgreSQL lexical search and RRF; scores remain relational and authoritative. Operator-only ingestion requires reviewed provenance. Corpus and evaluation files are unreviewed drafts, not published knowledge. Technical tests/live synthetic embedding/PG restore/API/UI smoke passed; full semantic evaluation, production identity and deployment-specific price snapshot configuration remain open. See `reports/us-03-syn-core-vector-rag-verification.md` and `../docs/architecture/syn-core-rag-implementation.vi.md`. No voice code was added.

### Syn coach A/B/C checkpoint — 2026-09-29

Latest user feedback authorizes chat to prioritize LLM explanation over visible evidence cards, a bounded second generation repair, and general educational answers when RAG lacks sources. Source-specific claims/citations still require actual evidence. DevOps/AI topics are supported for explanation/search without adding catalog simulations. Architecture details: `../docs/architecture/syn-coach-abc.vi.md`, verification `reports/us-03-syn-conversation-quality-verification.md`.

Approved continuation composes resource/simulation/plan capabilities, improves Vietnamese and ordered follow-up, retains same-path catalog fallback, retries Gemini 429/503 once within the deadline and checks broad competence claim patterns. Optional Google Search grounding provides turn-local W sources distinct from reviewed catalog and published K knowledge; only generic controlled topic queries reach search. Default execution is still server-planned with one generation pass, not unrestricted autonomous reasoning. Web results require consent and remain unreviewed; provider failure preserves local cards. RAG corpus drafts are not published without actual content review. See `../docs/architecture/syn-coach-abc.vi.md`, `reports/us-03-syn-coach-abc-verification.md` and `../docs/testing/syn-coach-manual.vi.md`.

### Approved Security/Auth and Core P1/P2 — 2026-10-19

This P1/P2 naming refers to Astra's Target v1.2 Master Plan, separately from the
historical Syn P1/P2 checkpoint. Scope is US01–08 / AC-S01–S07, R01, C01–C07 and
applicable D01. V19/V20 are additive security/learning changes after V18.
JWT/Google, account-scoped browser state, legacy actor/gate repair, ordered
learning/self-review, independent quiz scores/assessment methods and authorized
catalogue/enterprise workflows are described in
`../docs/architecture/p1-p2-security-core.md`. Syn/RAG/consent infrastructure stays;
P3–P6 and real deployment remain outside this implementation approval.

### Approved Agent Platform P3/P4 — 2026-10-20

The project lead approved US-09/10 packages P3/P4 after merged P1/P2. New
`guidance.knowledge` and `guidance.agent` subpackages add reviewed KB Studio,
immutable configurations, exact saved Admin preview, explicit assignment and
three-mode execution storage on additive V21. Existing Core/Syn, consent and
V1–V20 are preserved. Feature flags defaultfalse; no P5/P6 Student chat/history/
SSE/reports/plans/usage dashboard is implemented. Contracts and evidence:
`../docs/api/agent-platform-p3-p4.md`, `reports/p3-p4-completion.vi.md`.


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

</details>
