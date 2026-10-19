# Approved P1/P2: Security/Auth and Core alignment

Approval: project lead in the implementation chat, 2026-10-19. Requirements
Baseline v1 and Target Design v1.2 govern this increment. Earlier Package A and
Syn P1/P2 reports describe historical releases, not this release's numbering.

## Authentication boundary

`Browser -> Nginx -> SecurityConfig/BrowserAuthenticationFilter -> Identity facade
-> current account + credential version/revocations -> owning business service`.

- Google authorization code/OIDC uses Spring Security state, nonce, PKCE and
  issuer/signature/audience checks. The backend controls the provider endpoints
  and the configured exact redirect URI. Provider tokens are not retained.
- `external_identity(issuer, subject)` owns the mapping. A new Google subject with
  an existing email is refused. The user signs into the existing account, posts
  a CSRF-protected link intent, and completes Google authentication in the same
  live browser credential within five minutes. An email/domain never grants a
  role, membership, publishing permission or consent. New Google users are Students.
- Local password login remains available. OAuth-only accounts have no dummy
  password; local login refuses a null hash.
- `CAREERSIM_AUTH` is a signed HS256 JWT, HttpOnly, Secure by default, SameSite=Lax,
  path `/api`. Default lifetime is **900 seconds**; configurable bounds 60–1800.
  Browser code never receives the token body through a DTO and never stores it.
- JWT validation checks signature, issuer, audience, timestamps, jti, current
  authVersion, revocation and current ActorAccess. Role/membership/consent remain
  current server state. No session authentication fallback is accepted.
- Authentication is request-scoped. HttpSession is used only for CSRF/OAuth
  handshake. Login and successful OAuth rotate session ID and CSRF; ordinary JWT
  requests must not rotate CSRF. RequestAttributeSecurityContextRepository marks
  authenticated requests without saving an authenticated HttpSession.
- Logout revokes only the current jti until expiry and clears its cookie. Other
  devices stay signed in. Status/password/role changes increment authVersion via
  the additive DB trigger. Locked accounts are denied immediately; after unlocking,
  previous credentials remain invalid. There is no mandatory refresh-token service.
- Protected resource ownership uses the server actor. Foreign work is scoped to
  404; role/account denials are 403; absent/invalid/expired authentication is 401.
  Missing/invalid CSRF on browser writes is 403. Transitional identity headers can
  only agree with the actor; they cannot select a different owner.

Spring framework references consulted: [JWT validation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html),
[authorization grants/PKCE](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/authorization-grants.html),
[request versus session authentication strategy](https://github.com/spring-projects/spring-security/blob/6.5.x/config/src/main/java/org/springframework/security/config/annotation/web/configurers/SessionManagementConfigurer.java).

## Runtime configuration

`JWT_SIGNING_KEY` must come from secret storage: base64 of at least 32 random
bytes. Empty/short keys fail startup. Never commit a key or OAuth credentials.
`JWT_ALLOW_EPHEMERAL_KEY=true` explicitly enables a random process-local development
key; restarting invalidates its JWTs. This is used only in isolated verification.
`GOOGLE_OAUTH_ENABLED`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` and
`GOOGLE_REDIRECT_URI` configure real Google login. OAuth is unavailable when disabled.
There is no fake successful Google login in production code.

`SESSION_COOKIE_SECURE=true` controls both cookies. Use false only for an explicit
local HTTP test; production requires HTTPS/Secure. Proxy `Host $http_host` preserves
the browser's port. Do not expand CORS to work around a proxy dropping the origin's
port. The configured allowed origins must describe actual trusted deployments.

Existing authenticated sessions require re-login after this release; their
assessment, submission and consent history is preserved. Release/valuable DB
rollout, key management, real Google callback and multi-instance operational
verification remain separately gated.

## Core policy and persistence

V1–V18 remain byte-for-byte unchanged. V19 adds external identity, current
credential versions, per-jti revocation and nullable enterprise description.
V20 adds six learning sidecars and query indexes. It does not backfill history.
Core identifiers and normalized enrollment/submission/deliverable storage remain BIGINT.

New draft versions get `ordered-review/1` / `ORDERED_SELF_REVIEW`. New publications
require every task to be required; no optional skip behavior is invented. An
existing published version without sidecars retains legacy traversal and its
original hash. Cloning creates a new draft requiring the new publication policy.

`TaskProgressionPolicy` admits the first task, then task N only after every
predecessor is FINAL and has an explicit owner self-review. Locked tasks expose
an outline in enrollment, with no instructions/deliverables/resources. Submission
read/save/finalize, model answers and private task resources enforce the policy.

`quiz-exact/1` evaluates an optional reviewed rubric against a required JSON
choice deliverable. Score zero is a valid final submission. No rubric means no
invented score. Final, actor, optional score, completion and audit commit atomically.
Self-review is a separate immutable, owner-only, idempotent record. Last FINAL may
complete the simulation before its last review. Practical/text work has no
automatic grading prerequisite. Skills derive from actual FINAL task snapshots.

Parents/sidecars are locked against publication races. Published policies and
rubrics, final work, review/score/actor history and completion artifacts are immutable.
Existing fixed certificate/snippet semantics and purpose-specific evidence sharing
remain unchanged. Aggregate tenant statistics never return individual evidence.

Assessment supports frozen `riasec-sum/1` and independent `profile-sum/1` self-report
categories. Each attempt/result stays pinned to its type, version and methodology.
Neither is a composite career/proficiency score. Published content requires an
Admin action; this release does not invent production questionnaire content.

## Additive API contracts

- Existing `/v2/auth/{csrf,register,login,logout}`, `/v2/me`, Core enrollment,
  submission, model-answer, certificate and Syn URLs are retained.
- `GET /v2/auth/providers`, `POST /v2/auth/google/link`; Google start at
  `/api/oauth2/authorization/google`, callback `/api/login/oauth2/code/google`.
- `POST /v2/enrollments/{eid}/tasks/{tid}/self-review` acknowledges only own FINAL.
  Enrollment adds progressionMode/progression; task adds taskType; submission adds
  quizResult/reviewedAt/submittedByUserId. Historical unknown values remain null.
- `/v2/scenarios` retains the existing list shape. Opt-in `?paged=true` returns
  `{items,nextCursor}` with q, careerTrack, difficulty, sort, cursor, limit.
  Sort allowlist: ID_ASC, TITLE_ASC, DURATION_ASC, NEWEST, all with ID tie-break.
  Opaque cursors bind the same filters/sort. Detail adds a public taskOutline,
  never an answer or scoring rubric.
- Enterprise: `GET /v2/enterprise/scenario-versions` and existing draft/clone/save/
  preview/publish commands; `GET /v2/enterprises/{id}/participation-summary` is
  simulation-owned and uses EnterpriseAccess. Enterprise imports no evidence module.
- Admin: list all assessment types and their versions, get/save/publish exact
  versions; unpublished types remain accessible for resuming authoring.
- Legacy RIASEC/Career Quiz/attempt routes retain authorized history reads.
  Every legacy writer checks the flag at controller/service boundaries. Legacy
  simulation answer IDs must belong to the selected simulation before any write.

| Core v2 | Legacy writes | Behavior |
|---|---|---|
| false | false | Authorized legacy reads; legacy writes return WORKFLOW_UPGRADE_REQUIRED |
| false | true | Authorized legacy writes with real actor/CSRF; Core routes disabled |
| true | false | Core enabled; authorized legacy reads; old writers return upgrade conflict |
| true | true | Startup fails closed |

## Browser integration and ownership

Canonical typed assessment routes choose the configured workflow once. Shared API
calls use same-origin credentials and CSRF; account generations reject stale data
and pending writes after an account change. The routed private component tree and
Syn's account lifecycle reset on login/logout/401; cross-tab account changes notify
through a non-sensitive epoch. Draft keys include the authenticated owner.
Unowned old drafts are left untouched and are never automatically adopted.

Typed assessment slugs match type codes case-insensitively with hyphens and
underscores treated equivalently (`work-activities` → `WORK_ACTIVITIES`). An
unpublished/unknown requested type does not silently start the first published
type. Changing a typed route remounts the workflow; saved owner drafts remain.

Core final submission reuses `SubmissionConfirmationDialog` with the task-specific
irreversibility warning, cancel/escape, focus return and keyboard focus trapping.
The final API command is sent only after explicit confirmation; no automatic
confirmation or native browser dialog bypass is used. Locked-task projections
also strip translated instructions/context while retaining translated titles.

New `/enterprise` and `/admin/assessments` pages are necessary P2 integration.
Existing feature owner coordination remains Bích (assessment), Trang (simulation),
Hân (guidance/shared); no ownership transfer or unrelated redesign is claimed.
EN/VI mappings, semantic controls and server error feedback follow existing conventions.

P3–P6/KB Studio/Agent Builder are not implemented here. Syn orchestration, consent
admission, pgvector, evidence tools and deterministic fallback remain in place.
Any next migration must discover the then-current head; V19/V20 are now P1/P2,
so historical Package B numbering is no longer a reservation.
