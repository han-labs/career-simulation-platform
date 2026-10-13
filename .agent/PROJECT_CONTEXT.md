# Career Simulation Platform Project Context

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

Approved implementation now includes strict shared output checks, bounded/cancellable HTTP, short snapshot/begin/finalize transactions, durable idempotent turns, ownership/consent rechecks, clear/expiry races, direct older-evidence lookup, and tab-only transcript. A/S references identify authorized completed attempts; K references identify published knowledge chunks and versions. V10–V12 add lifecycle, pgvector knowledge and trace metadata. Retrieval uses pinned 768-dimensional Gemini embeddings plus PostgreSQL lexical search and RRF; scores remain relational and authoritative. Operator-only ingestion requires reviewed provenance. Corpus and evaluation files are unreviewed drafts, not published knowledge. Technical tests/live synthetic embedding/PG restore/API/UI smoke passed; full semantic evaluation, production identity and deployment-specific price snapshot configuration remain open. See `reports/us-03-syn-core-vector-rag-verification.md` and `../docs/architecture/syn-core-rag-implementation.vi.md`. No voice code was added.

### Syn coach A/B/C checkpoint — 2026-09-29

Latest user feedback authorizes chat to prioritize LLM explanation over visible evidence cards, a bounded second generation repair, and general educational answers when RAG lacks sources. Source-specific claims/citations still require actual evidence. DevOps/AI topics are supported for explanation/search without adding catalog simulations. Architecture details: `../docs/architecture/syn-coach-abc.vi.md`, verification `reports/us-03-syn-conversation-quality-verification.md`.

Approved continuation composes resource/simulation/plan capabilities, improves Vietnamese and ordered follow-up, retains same-path catalog fallback, retries Gemini 429/503 once within the deadline and checks broad competence claim patterns. Optional Google Search grounding provides turn-local W sources distinct from reviewed catalog and published K knowledge; only generic controlled topic queries reach search. Default execution is still server-planned with one generation pass, not unrestricted autonomous reasoning. Web results require consent and remain unreviewed; provider failure preserves local cards. RAG corpus drafts are not published without actual content review. See `../docs/architecture/syn-coach-abc.vi.md`, `reports/us-03-syn-coach-abc-verification.md` and `../docs/testing/syn-coach-manual.vi.md`.
