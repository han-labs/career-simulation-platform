# Frontend walkthrough: US-03 Dashboard and global Syn widget

This is the presentation script for the current implementation. The product
copy is English; the explanations below are intentionally concise so the
owner can trace each responsibility without memorising generated boilerplate.

## 1. What the user sees

- `Dashboard` summarises progress, RIASEC signals, practised skills, the latest
  objective simulation result, and the current exploration plan.
- Syn is mounted once for the entire application. It starts as a circular
  launcher, opens as a compact chat, and can expand to a larger chat window.
- The conversation remains in memory while the user changes routes because
  the widget is above individual pages in the React route layout.
- Quick actions produce deterministic Standard guidance. Eligible free-text
  questions may receive an AI-assisted answer; both are labelled visibly.
- Syn can explain evidence and draft a plan, but cannot score a task, certify a
  skill, choose a career, or save a draft without confirmation.

## 2. Small architecture map

```text
Browser
  -> gateway Nginx (:8080)
     -> React bundle -> ApplicationLayout
        -> AppShell -> Outlet -> current page
        -> SynWidget -> one global popup on every route
     -> /api/v1/guidance/** -> GuidanceController
        -> GuidanceServiceImpl
           -> evidence access + GuidanceDao/PostgreSQL
           -> StandardGuidancePolicy
           -> SynGuidanceProvider -> OpenAI Responses API (eligible free text only)
                                 \-> deterministic fallback (every failure)
```

The important boundary is that React never receives the provider key and does
not decide whether AI is allowed. Consent, input policy, ownership, provider
availability, fallback, and audit metadata are backend responsibilities.

## 3. Request-to-render flow

### Opening any page

1. Nginx serves `index.html`; `frontend/nginx.conf` falls back to it for SPA
   routes such as `/dashboard`.
2. `main.jsx` creates the React root and enables `BrowserRouter`.
3. `App.jsx` renders `ApplicationLayout`.
4. `ApplicationLayout` always renders `AppShell` and `SynWidget` as siblings.
5. `AppShell` puts the route page in `<Outlet />`; Syn stays mounted when that
   route page changes.
6. The initial widget render returns only the circular launcher, so Dashboard
   data is not requested until Syn is opened.

### Opening and resizing Syn

1. `mode` begins as `collapsed`.
2. Clicking the launcher changes it to `compact` and lazily loads Dashboard
   context.
3. The maximise button toggles `compact` and `expanded`.
4. The minus button or Escape changes it back to `collapsed`.
5. CSS selectors `.syn-widget--compact` and `.syn-widget--expanded` control
   size; the React component and data flow remain the same.

### Asking a question

1. `draft` is a controlled textarea value.
2. Submit prevents a page refresh and calls `askSyn`.
3. The user message is appended immediately; `isReplying` prevents duplicate
   requests.
4. `guidanceApi.sendSynMessage` POSTs the validated message, action type, and
   only the current attempt identifier.
5. The backend returns a normalized `SynMessageResponse` with provenance.
6. If the endpoint is unavailable/rate-limited or the response is invalid,
   the adapter returns a deterministic Standard reply.
7. Updating `messages` causes React to reconcile the message list and the
   browser paints the new DOM with the existing CSS.

### Dashboard actions

`GuidancePage` does not import the chat component. It dispatches the scoped
`careersim:syn-open` browser event with a message and action type. The single
global widget listens to that event, opens, loads context if necessary, and
submits the action. This avoids a global state library and an extra context
file for one narrow cross-component interaction.

### Drafting and saving a plan

1. `DRAFT_PLAN` is deterministic and returns a structured `planDraft`.
2. `SynMessage` renders it as a card; no write occurs yet.
3. `Review before saving` sets `pendingPlan`.
4. Only `Confirm and save` calls `saveExplorationPlan`.
5. Preview mode explicitly says nothing was saved; a live response confirms
   persistence. Assessment and simulation results are never mutated.

## 4. Files to understand

| File | Responsibility |
|---|---|
| `frontend/src/main.jsx` | Mount React and BrowserRouter; import global CSS |
| `frontend/src/app/App.jsx` | Route table and the layout that mounts Syn globally |
| `frontend/src/shared/layout/AppShell.jsx` | Shared navigation, Outlet, and footer |
| `frontend/src/features/guidance/pages/GuidancePage.jsx` | Concise Dashboard state and rendering |
| `frontend/src/features/guidance/components/SynWidget.jsx` | Three modes, session messages, quick actions, plan confirmation |
| `frontend/src/features/guidance/api/guidanceApi.js` | HTTP contract, normalization, preview and Standard fallback |
| `frontend/src/shared/api/httpClient.js` | Base URL, fetch, API envelope, safe `ApiError` |
| `frontend/src/styles/index.css` | Dashboard/widget hierarchy and responsive rules |
| `frontend/src/features/guidance/api/guidanceApi.test.js` | Adapter decision-boundary tests |
| `frontend/nginx.conf` | Production SPA serving and browser security headers |
| `deployment/nginx/default.conf` | API routing, Syn rate limit, body limit, security headers |

Only one new frontend component file was introduced. Small helper components
(`SourceBadge`, `ResultCard`, `SynMessage`) remain in `SynWidget.jsx` because
they are not reused elsewhere.

## 5. Reading `SynWidget.jsx` from top to bottom

- Imports: visual icons, React hooks, and three feature API functions.
- Constants: one custom event name, four quick actions, and one welcome
  message. They are data, so the JSX does not repeat four button blocks.
- Pure helpers: status label, provenance badge, objective result card, and
  message renderer. They do not fetch or mutate global state.
- State: window `mode`, Dashboard context, load error, session `messages`,
  textarea `draft`, request lock, and the `pendingPlan` confirmation boundary.
- Refs: focus the composer and scroll the conversation without querying the
  whole document.
- `loadDashboard`: one reusable async loader for opening and retry.
- `showWidget`: opens compact mode and performs lazy loading.
- `askSyn`: validates local state, appends messages immutably, calls the
  adapter, and always releases the request lock in `finally`.
- Effects: listen/clean up the Dashboard event; focus after opening; scroll
  after a message; listen/clean up Escape.
- `confirmPlan`: the only plan-write boundary in the widget.
- Render branches: one small button when collapsed; otherwise one accessible
  non-modal dialog containing header, state, conversation, actions, and form.

## 6. Knowledge needed to explain the code

- React `useState`: data that triggers a re-render when its setter is called.
- React `useEffect`: browser-side work after render; every event listener has
  a cleanup function.
- React `useCallback`: keeps async handlers stable enough for effect
  dependencies without moving them to another file.
- React `useRef`: references a DOM element without creating render state.
- Controlled input: `value` comes from state and `onChange` updates it.
- Immutable list update: `[...current, newItem]` creates a new array so React
  sees the change.
- Conditional render: `condition && JSX`, ternaries, and early returns choose
  the smallest valid UI state.
- Optional chaining and nullish fallback prevent missing optional DTO fields
  from crashing the browser.
- `CustomEvent`: a deliberately tiny bridge from Dashboard to the globally
  mounted widget; it is not used for business data storage.
- CSS Grid/Flexbox, fixed positioning, media queries, `:focus-visible`, ARIA,
  and `prefers-reduced-motion` cover layout and accessibility.

## 7. Why token use stays controlled

- Opening Syn and every quick action costs zero model tokens.
- Only `FREE_TEXT` can reach the provider, after recorded AI consent.
- The browser shows session history for usability but sends no chat history.
- The backend sends only the current message plus at most three interest
  signals, three skills, three results/outcomes, and a plan summary.
- Provider output is capped and must satisfy a strict JSON schema.
- `store=false` asks the provider not to retain the response object.
- Missing key, unsafe input, timeout, refusal, malformed output, or provider
  failure returns Standard guidance instead of retrying repeatedly.

## 8. Security boundaries to mention

- The key exists only in backend environment configuration.
- Guidance routes remain authenticated and the service requires an ACTIVE
  STUDENT and checks result ownership.
- The server validates length and allowlists action types.
- The policy catches requests for system instructions, answer keys, secrets,
  guarantees, and career decisions before a provider call.
- Raw prompts, keys, and raw provider errors are not logged or returned.
- Gateway limits reduce flooding and oversized requests; CSP, frame denial,
  MIME-sniffing, referrer, and permission headers reduce browser attack
  surface.

The honest limitation: production identity is not implemented yet, so local
browser calls currently use clearly labelled preview/Standard mode. Do not
make guidance public just to demo AI. The opt-in backend smoke test exercises
the real provider with synthetic data until authenticated end-to-end wiring is
available.

## 9. Short demo script

1. Open `/`, `/assessment`, then `/simulations`; point out the same Syn circle.
2. Open compact mode, expand it, return to compact, then press Escape.
3. Open `/dashboard`; show that the page contains evidence only, not a second
   embedded chat.
4. Click `View details with Syn`; show the popup and the unchanged objective
   score.
5. Run `Find evidence gaps`; explain that it requests more evidence rather
   than declaring a weakness.
6. Run `Draft a next-step plan`; show the explicit save confirmation.
7. Ask free text; explain AI eligibility and the visible provenance badge.
8. Resize below the mobile breakpoint and show that the widget stays within
   viewport and buttons remain reachable.

## 10. Commands to demonstrate quality

```powershell
cd frontend
npm run check
```

This runs lint, unit tests, and the production build. For the full stack:

```powershell
cd ..
docker compose config --quiet
docker compose up -d --build
docker compose ps
Invoke-RestMethod http://localhost:8080/api/health
```

Questions the presenter should be able to answer:

1. Why does Syn remain mounted when the route changes?
2. Which actions spend tokens, and why do quick actions not do so?
3. Where are consent, ownership, and prompt policy enforced?
4. How does malformed provider output become Standard guidance?
5. Why can Syn display a score but never change or recompute it?
