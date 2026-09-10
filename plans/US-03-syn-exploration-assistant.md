# US-03 Syn Exploration Assistant Feature Plan

Status: Frontend implemented; backend integration pending
Owner: Huỳnh Gia Hân
Date: 2026-09-11

## 1 Scope

- User story: As a student, I want to interact with Syn, an evidence-grounded exploration assistant, so that I can reflect on my assessment and simulation experiences, understand what I have practised, and form my own possible next steps without the system making a career decision for me.
- Actor: Student.
- Source: `Proposal_CareerSimulationPlatform_Updated.docx`, sections 1.2, 1.2.1, 1.4, 2.3, 2.6, and 2.7.
- Acceptance criteria: Syn introduction and limitations; evidence-grounded RIASEC/result explanation; interest and skill comparison; inline structured result cards; optional next activities; confirmed plan saving; no scoring, answer-key disclosure, prediction, certification, or final decision; visible deterministic fallback.
- Explicit exclusions: standalone Skill Evidence Report, separate ResultDetailPage, backend AI implementation, authentication, persistence, vector database, autonomous loops, and career prediction.
- Related owner boundaries: this change implements only the US-03 frontend and shared route/navigation changes; US-01 and US-02 workflows remain owned by their assigned contributors.

## 2 Flow inventory

### Main success flow

1. The student opens `/dashboard` and sees concise progress, RIASEC signals, practised skills, recent results, and the current plan.
2. The student selects a context-aware quick action or writes a message to Syn.
3. Syn displays a response with provenance and, when relevant, a deterministic result-detail card.
4. A plan draft is presented for review and is never silently saved.

### Alternate flows

1. Missing assessment, skill, result, or plan data is presented as a calm empty state.
2. `/guidance` remains a compatibility route and renders the same US-03 page.
3. On narrow screens, Dashboard and Syn stack into one readable column.

### Exception and fallback flows

1. While the backend contract is unavailable, the page uses clearly labelled preview data and Standard guidance.
2. Failed requests preserve the current conversation and expose retry/recovery copy without raw provider errors.
3. Objective scores in result cards are copied from backend/preview evidence and are never calculated or changed by Syn.

## 3 Responsibility design

| Responsibility | Expert/creator/controller | Proposed class/component | Reason |
|---|---|---|---|
| Render and coordinate US-03 state | React page controller | `GuidancePage` | Keeps the student flow visible in one explainable file. |
| Define frontend/backend contract and preview fallback | API adapter | `guidanceApi.js` | Keeps HTTP and response normalization outside the React view. |
| Shared navigation and responsive presentation | App shell / CSS | `AppShell`, `index.css` | Reuses the existing application shell and design tokens. |

## 4 Change surface

- Frontend: `/dashboard`, `/guidance`, `GuidancePage`, `guidanceApi.js`, navigation, landing journey label, and feature CSS.
- Backend/API: no backend implementation in this task; frontend targets `GET /v1/guidance/dashboard` and `POST /v1/guidance/syn/messages` through the existing `/api` base URL.
- Database/migrations: none.
- Documentation: synchronize the US-03 requirement wording and record this plan.

## 5 API contract

| Method | Path | Request | Success | Error codes |
|---|---|---|---|---|
| GET | `/api/v1/guidance/dashboard` | none | dashboard summary DTO | network, unauthorized, unavailable |
| POST | `/api/v1/guidance/syn/messages` | `{ message, actionType, context }` | structured Syn message DTO | validation, timeout, unsafe, unavailable |

## 6 Security privacy and AI

- Authentication/ownership: backend must derive the student from authenticated context; the frontend sends no hard-coded user ID.
- Sensitive data: no answer keys, internal prompts, credentials, or unrelated profile history are rendered or requested.
- AI context: only the selected assessment/result/plan context identifier is sent.
- Output: frontend expects text, provenance, optional evidence/result card, options, and optional plan draft.
- Fallback: Standard guidance remains visible and objective results remain unchanged.
- Logging: raw prompts, provider errors, and private evidence are not written to the browser console.

## 7 Tests and evidence

- Frontend tests: dashboard normalization, AI response normalization, deterministic fallback, and objective score stability.
- Manual evidence: desktop and mobile checks for Dashboard, quick actions, free text, result cards, empty/fallback labels, keyboard focus, and no horizontal scrolling.
- Commands: `npm run check`, `docker compose config --quiet`, and local browser smoke through Nginx when the stack is available.

## 8 Risks and decisions

| Question/risk | Impact | Recommendation | Approval |
|---|---|---|---|
| Backend US-03 endpoints are not implemented. | Live data cannot be demonstrated yet. | Use explicit Preview data and Standard guidance while keeping the adapter contract stable. | Approved for frontend-first implementation. |
| A separate result-detail page increases scope. | More routing and duplicated presentation. | Keep deterministic result details inside Syn cards. | Approved. |
| Too many frontend abstractions reduce explainability. | Harder course presentation and ownership. | One page, one adapter, one test file, and shared CSS only. | Approved. |

## 9 Verification evidence

- `npm run check`: passed (lint, 8 tests, production build).
- `backend/mvnw.cmd verify`: passed (7 backend and architecture tests).
- `docker compose config --quiet`: passed; Nginx/Docker smoke used the rebuilt frontend at `/dashboard`.
- Browser checks: Dashboard preview and provenance label visible; inline result kept the objective `80/100` score; plan required review and confirmation; preview save explicitly wrote nothing.
- Responsive checks: desktop at 1280 px and narrow layouts at 390 px and 320 px had no horizontal overflow; Dashboard cards, Syn, quick actions, and mobile navigation stacked correctly.
