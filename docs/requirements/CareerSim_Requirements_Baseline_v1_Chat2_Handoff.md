# CareerSim — Requirements Baseline v1

**Handoff:** Chat 3 (Requirements / BA) → Chat 2 (Diagram & Code Mapping) · **Date:** 2026-10-19
**Status:** **FROZEN SCOPE v1** theo DR1–DR2 và các business rules đã xác nhận; các mục gắn **PROPOSED / OPEN** bên dưới chưa phải quyết định được duyệt. **Không phải chứng nhận code đã hoàn thành.**
**Order of authority:** `Final_Project_Assignment_2026-2027.docx` (đề chấm thi) → quyết định người dùng **CONFIRMED** → business rules đã chốt trong report cũ → `CareerSim_AI_Agent_Requirements_Handoff_v4.md` và ba sơ đồ `.mmd` (**TARGET DESIGN / discussion draft**) → giải pháp kỹ thuật **PROPOSED**. Nếu xung đột, không tự sửa business rules.

## 1. Scope freeze

### MUST-HAVE — nghiệm thu cuối kỳ

**Core workflow:** (1) OAuth2 login, JWT-secured REST và RBAC Student/Enterprise/Admin; tài khoản/enterprise publishing permissions. (2) Student profile/dashboard; Admin quản lý assessment types/versions; Student chọn **assessment tùy chọn**, xem nhiều assessment results đúng từng loại. (3) Enterprise tạo, sửa, preview, publish versioned scenario/task/model answer trong đúng tenant; Student tìm/duyệt simulation có search/filter/sort/pagination, tự do tham gia không cần assessment. (4) Task theo thứ tự, draft/resume, quiz có điểm nhưng **nộp hợp lệ là hoàn thành** không cần điểm tối thiểu; task tự luận/thực hành không auto-grade, nộp xong mới mở model answer, Student tự so sánh rồi chuyển task tiếp. Kỹ năng **đã thực hành** gắn task hoàn thành xuất hiện ở kết quả, không phải chứng nhận thành thạo. (5) Completion, lịch sử, achievement/certificate và resume snippet trung thực; enterprise xem thống kê tham gia tổng hợp trong phạm vi tổ chức, không xem dữ liệu cá nhân trái quyền/consent.

**AI Platform & Student Agent:** (6) Admin-only KB Studio: quản lý KB, upload/ingest/publish nguồn KB, inspect evidence và Test KB độc lập; dùng KB có scope/grants, provenance; tài liệu đã chốt hướng hỗ trợ **TXT, PDF, DOCX, MD, XLSX**, thực hiện theo slice có acceptance cho từng loại. (7) **Admin Agent Builder**: create/edit/configure Agent, model, prompt, KB grants, tool toggles, policy, save immutable config version, preview/test, publish và **assign Student**; Student không sửa cấu hình. (8) **A+B+C**: RAG chunk/embedding/vector retrieval có nguồn và biết nói không biết; model thực sự gọi **≥3 read-only domain tools** trên dữ liệu CareerSim, có KB search bổ sung; output JSON kiểm tra schema và được ứng dụng dùng. (9) Agent giải thích catalogue/loại assessment trước khi Student làm, giải thích các simulation công khai trước khi tham gia, phân tích các kết quả assessment/simulation riêng sau khi làm, phân biệt nguồn và suy luận, không lộ model answers chưa unlock. (10) Student **yêu cầu** tạo `InsightReport` có cấu trúc; validate/persist/provenance và chỉ owner đọc; AI đề xuất `ExplorationPlan` nhưng không tự lưu thành plan chính thức khi Student chưa xác nhận. (11) Tự lưu chat để resume, context summarization và cá nhân hóa nhẹ theo Student (không train model); Student xem/xóa lịch sử. External LLM processing cần consent phù hợp, giới hạn dữ liệu chia sẻ. (12) AI usage dashboard Admin: số requests, estimated cost, average latency, failed/refused; per-request logging prompt version, tool calls, tokens, latency; fallback và guardrails.

**Bắt buộc xuyên suốt từ assignment:** React web responsive, Spring Boot, PostgreSQL+pgvector, backend-only model API, protected secrets, REST CRUD, file upload phục vụ AI, **SSE streaming** vừa làm modern frontend feature vừa đáp ứng real-time (**PROPOSED implementation choice**), Docker Compose/deployed demo, unit/integration/safety tests, 20 AI eval cases + before/after, AI_USAGE.md, 3 ADR, traceability và đầy đủ deliverables/defense.

### DEFERRED — KHÔNG tính để chứng minh MVP cuối kỳ

Student private CV/document uploads vào chat; multimodal capability **D**; agent-to-agent critique/multi-agent **E**; Enterprise Agent và Enterprise AI scenario authoring; generic `AgentActionProposal` cho scenario/task writes; autonomous write tools; auto-scoring bài tự luận / proficiency certificate; proprietary model training; advanced unsupervised cross-session long-term memory; recruitment invitation/full hiring process; VR/AR, LMS, research analytics, billing. **Lưu ý:** dữ liệu/versioning/permission trong TARGET DESIGN có thể chuẩn bị để mở rộng, nhưng không bắt buộc build các workflow deferred.

### Trạng thái và ranh giới

- **CONFIRMED:** DR1-01 A+B+C; DR1-02 quiz scores + completion-based practiced skills và không auto-grade tự luận; DR1-03 Admin KB upload; DR1-04 structured report + user-confirmed plan; DR2-01 Student Agent tra cứu assessment/catalogue/multiple results và simulation/catalogue/own outcomes, dùng KB; DR2-02 submission → model answer → self-review → next task; DR2-03 report on request, owner-only; DR2-04 chat tự lưu, resume/delete, external AI consent; DR2-05 cá nhân hóa bằng bounded history/context summary, không ép mốc 30/90 ngày.
- **TARGET DESIGN:** Core v4, AI Agent v4 và ERD v4 — logical models, không phải SQL/Java hiện tại.
- **REPORTED AS-BUILT (chưa audit HEAD):** Package A/Core có Flyway V1–V18; Syn/RAG/consent/cancellation một phần đã có. Package B/Configurable Agent Platform **chưa triển khai**. Không khẳng định status READY/DONE trên cơ sở diagram.
- **PROPOSED kỹ thuật không cần BA hỏi lại:** Google OAuth2 + backend JWT, SSE streaming, 3 domain read-only tool contracts, scoped immutable evidence, bounded summary, least-privilege/delete propagation, PostgreSQL JSONB cho output/trace; áp dụng theo sau repository audit.
- **OPEN nhưng không chặn viết user stories:** Quy định lưu giữ backup/audit cụ thể, cách thi hành xóa nội dung dẫn xuất và governance của inter-session summaries; hoàn thành privacy/retention policy trước triển khai thực tế dữ liệu người dùng. Không tự gắn 30/90 ngày.

## 2. User Stories và Acceptance Criteria (15 stories)

> AC là **hành vi nghiệm thu**, không phải lời khẳng định đã code. `UC`/`FR` chỉ đến **report cũ** để tái sử dụng; tên endpoints, file source và test path sẽ được mapping ở Chat 2/implementation.

| ID / Actor / reuse | User Story | Acceptance Criteria (ngắn, testable) |
|---|---|---|
| **US-01** · Student / Enterprise / Admin · UC-01/02/12; FR-S01/02, E01/02, A01/02 | Là người dùng, tôi muốn đăng nhập an toàn và chỉ thấy chức năng đúng vai trò. | **AC1:** OAuth2 login phát hành/thiết lập JWT cho protected REST; token invalid/expired → 401, sai quyền/tenant → 403. **AC2:** Admin có thể khóa tài khoản, duyệt/hạn chế quyền Enterprise publish; quyền thay đổi có hiệu lực server-side và có audit. |
| **US-02** · Student · UC-03/04; FR-S03/04 | Tôi muốn quản lý profile và xem dashboard khám phá của mình. | **AC1:** Student cập nhật các trường hợp lệ và chỉ đọc/sửa profile thuộc mình. **AC2:** Dashboard phân biệt assessment results, simulation progress, practiced skills, exploration history; không diễn giải đó là chứng nhận năng lực. |
| **US-03** · Admin · UC-13; FR-A03/A04 | Tôi muốn quản lý assessment catalogue/content theo phiên bản để Student chọn bài phù hợp. | **AC1:** Admin CRUD bản draft, validate rồi publish assessment type/questions/evaluator config; bản đã dùng vẫn truy nguyên được version. **AC2:** Config không hợp lệ không publish và không phá bản active. |
| **US-04** · Student · UC-05; FR-S05/06/07 | Tôi muốn tự chọn và thực hiện nhiều loại assessment, xem từng kết quả. | **AC1:** Chưa làm assessment vẫn tham gia simulation được. **AC2:** Submit đủ dữ liệu → lưu attempt, assessment version và result; nhiều attempts/types hiển thị riêng, không tạo unsupported composite career score. |
| **US-05** · Enterprise · UC-09/10; FR-E03–E08, A04 | Tôi muốn tạo và publish các simulation thực tế cho Student. | **AC1:** Enterprise được cấp publishing right có thể CRUD/draft/preview ordered versioned tasks, deliverables, model answers, skill codes; thiếu thông tin không publish. **AC2:** Enterprise chỉ thấy/chỉnh content của tổ chức và xem **aggregate** participation của mình; individual evidence/contact chỉ khi đúng quyền và consent. |
| **US-06** · Student · UC-06; FR-S08/09 | Tôi muốn tìm và xem simulation trước khi đăng ký. | **AC1:** Chỉ các published/accessible simulations được liệt kê; search, filter, sort, pagination hoạt động và stable. **AC2:** Detail có workplace/role/objectives/task outline nhưng **không trả model answer chưa unlock**; Student được tự chọn bất kỳ simulation hợp lệ. |
| **US-07** · Student · UC-07; FR-S10–S13 | Tôi muốn làm task tuần tự, nộp rồi xem ví dụ để tự đối chiếu. | **AC1:** Save draft/resume và finalize task idempotent; quiz có điểm nhưng hoàn thành bằng valid submission, tự luận không auto-grade. **AC2:** Model answer chỉ mở sau khi Student được phép xem theo task; trước đó UI/API/Agent đều không tiết lộ; hoàn thành task cho phép tiến đến bước tiếp theo. |
| **US-08** · Student · UC-03/07; FR-S14/17 | Tôi muốn xem kết quả simulation và bằng chứng hoàn thành. | **AC1:** Sau khi hoàn thành required tasks, Student xem completion và **practiced skills theo completed tasks**; không tuyên bố verified proficiency. **AC2:** Có thể xem lại task evidence và nhận certificate/resume snippet mô tả chính xác trải nghiệm, không bịa internship/employment. |
| **US-09** · Admin · UC-13 mở rộng; DR1-03 | Tôi muốn quản lý/tải tài liệu vào KB và kiểm thử tri thức trước khi dùng. | **AC1:** Admin tạo/sửa/ẩn KB, upload file hợp lệ; ingest có status và provenance; invalid/parser failure không làm hỏng published version. **AC2:** KB Test trả ranked passages thật và câu trả lời có source/version/locator; private/tenant KB ngoài grant không được truy xuất; RAG thiếu bằng chứng → nói không biết. |
| **US-10** · Admin · UC-13 mở rộng; Agent v4 | Tôi muốn cấu hình, test, publish và assign Agent cho Student. | **AC1:** Save cấu hình gồm model/system prompt, KB grants, tool allowlist, policies tạo **immutable version**, không âm thầm đổi chat đang chạy. **AC2:** Preview/test trước publish; Student chỉ thấy/chạy Agent active được assign; Student không quản lý KB, prompt hoặc tools. |
| **US-11** · Student · UC-08; FR-S15/16; AI-A/B | Tôi muốn hỏi Agent về assessment, simulations và tình trạng khám phá của mình để nhận gợi ý có căn cứ. | **AC1:** Trước/sau assessment/simulation đều hỏi được; LLM **thực sự gọi ≥3 approved read-only domain tools** trong bộ demo về assessment catalogue/results, simulation catalogue/details và own participation/evidence; `kb.search` retrieval có citations riêng. **AC2:** Không lộ answers chưa unlock, người/tenant khác; unsupported evidence → thừa nhận thiếu dữ kiện; stream AI qua SSE; prompt injection/out-of-scope/provider failure → an toàn, Core vẫn dùng được. |
| **US-12** · Student · UC-08 mở rộng; AI-C; DR1-04/DR2-03 | Tôi muốn yêu cầu tạo báo cáo phân tích nghề nghiệp để xem lại. | **AC1:** Khi Student yêu cầu, LLM trả **schema-validated JSON**; invalid/empty JSON không được publish như report hợp lệ. **AC2:** Một `InsightReport` được lưu với owner, source execution, evidence và AI-generated disclaimer; owner xem lại không gọi lại model, user khác/Admin thường không đọc nội dung cá nhân. |
| **US-13** · Student · UC-08/FR-S17 mở rộng; DR2-04/05 | Tôi muốn tiếp tục trò chuyện và xóa lịch sử của mình. | **AC1:** Lịch sử từng session được lưu theo thứ tự, resume đúng owner; bounded context summary hỗ trợ multi-turn và lightweight student-specific personalization nhưng không suy diễn thành verified facts. **AC2:** Xóa session làm chat không truy xuất/sử dụng lại được; các derived summaries chỉ từ nội dung bị xóa phải vô hiệu hóa; external AI calls cần consent hiện hành. |
| **US-14** · Student · UC-08 mở rộng; DR1-04 | Tôi muốn xem đề xuất exploration plan và tự quyết định có lưu hay không. | **AC1:** AI tạo **draft suggestion**; không cập nhật plan chính thức trước hành động xác nhận rõ. **AC2:** Khi xác nhận, backend tái kiểm tra owner/permission và lưu plan; bỏ qua/từ chối không tạo business write. |
| **US-15** · Admin · FR-A04 mở rộng; Assignment §6 | Tôi muốn theo dõi mức sử dụng và chất lượng vận hành AI. | **AC1:** Dashboard hiển thị số request, estimated/known cost, average latency, số failed/refused theo khoảng thời gian; tổng hợp từ persisted request metrics. **AC2:** Log truy nguyên model/prompt version, invoked tools, tokens và latency, không hiển thị trái phép private prompt/report content. |

**Tool contract mapping đề xuất (không khóa Java names):** `assessment.lookup` → catalogue + authorized own attempts/results; `simulation.explore` → public published scenarios/tasks metadata; `simulation.myEvidence` → own enrollment/submissions/completion/quiz scores/practiced skills; `kb.search` → authorized published KB passages. **Ba domain tools đầu phải được model thực sự gọi qua tool-calling**, không phải chỉ được backend chạy cố định; KB tool là bổ sung. Read-only tools ở MVP. Prompt provider/LLM không được tự áp dụng business changes.

## 3. Full Instructor Coverage Checklist — Section 1–11

Các hàng là **requirement phải có**, không phải báo cáo đã implement. Evidence `TBD` cho đến khi code/test/demo xác minh.

| Req / Assignment | Điều kiện phải chứng minh | Coverage baseline | Evidence / status |
|---|---|---|---|
| **S1–2 Product / team** | AI giải quyết bài toán thật và làm việc trên data/tool, không chỉ LLM proxy; nhóm ≤3 | US-04/06/07/11/12; Student career exploration | Working demo **TBD** |
| **S3 Web frontend** | Chọn **Web React**, responsive | US-01–15 | UI/browser test **TBD** |
| **S3 Modern feature** | ≥1 modern frontend feature | **SSE streamed AI response** (**PROPOSED**) tại US-11 | Stream browser demo **TBD** |
| **S3 Backend** | Spring Boot; protected REST CRUD **JWT** | US-01/03/05/09/10 | OAuth/JWT/API integration tests **TBD** |
| **S3 Database/vector** | Persistent SQL/NoSQL; RAG có vector store | PostgreSQL + pgvector; US-04/07/09/13 | DB/vector query test **TBD** |
| **S3 LLM/secrets** | LLM calls only backend; không API key frontend hoặc Git | US-10/11/12; server adapter / env secret | secret scan/config verification **TBD** |
| **S3 Login/roles** | OAuth2 (Google/GitHub/Facebook) + RBAC admin/user | US-01; **Google OAuth2 PROPOSED** | OAuth login, JWT, forbidden-role tests **TBD** |
| **S3 Real-time** | WebSocket/SSE/etc hoặc token streaming | US-11 SSE (**PROPOSED**) | streaming proof **TBD** |
| **S3 Deployment** | Docker Compose hoặc hosting; demo trên deployed/containerized app | Deployment requirement cross-cutting | docker compose up + deployed demonstration **TBD** |
| **S4.1 A — CONFIRMED** | Chunking, embeddings, vector search, cited answers, don't-know when absent | US-09/11 | RAG pipeline + corpus negative test **TBD** |
| **S4.1 B — CONFIRMED** | LLM **calls ≥3 tools** accessing real app data; every write confirmed | US-11/14; 3 domain tools above, human-confirmed plan | Native function-call/tool traces & unauthorized tests **TBD** |
| **S4.1 C — CONFIRMED** | LLM JSON schema validated **and used in app logic** | US-12 InsightReport; US-14 optional plan | valid/invalid schema + persisted/report use test **TBD** |
| **S4.1 D/E** | Not selected; only 3 of A–E required for 3 members | **DEFERRED** | Không dùng D/E để claim điểm |
| **S4.2 Model choice** | Trình bày model, quality/cost/speed/privacy tradeoffs | ADR/model selection; provider may be Gemini-first **PROPOSED** | ADR + benchmark **TBD** |
| **S4.2 Prompt management** | Prompt ở file/config; **version history in Git** | US-10 saved config + source-controlled prompt templates/policies | config & Git revisions **TBD** |
| **S4.2 Guardrails** | Injection; out-of-scope; wrong/failed response (timeout/invalid JSON/empty) **đều demo được** | US-09/11/12 | ≥3 named demo/test cases **TBD** |
| **S4.2 Evaluation** | ≥20 test cases normal/edge/should-refuse; expected results; pass-rate **before/after** 1 improvement | A/B/C eval set + quality report | test matrix & two dated runs **TBD** |
| **S4.2 Logging/cost** | Per request prompt version/tools/tokens/latency; average cost/time | US-15 | measured dashboard/export **TBD** |
| **S4.2 Fallback** | Non-AI flows work during AI outage; clear AI error | US-07/11/12 | provider-offline regression **TBD** |
| **S5.1 Stories/traceability** | 8–15 user stories with AC; `US → endpoint/component → source file → tests` | **15 US above**; mapping skeleton in §4 | Complete after Chat 2 audit/code **TBD** |
| **S5.2 Architecture** | UI, backend, DB, vector store, LLM and **one AI request data flow** | Target architecture diagram deliverable | diagram **TBD** |
| **S5.2 ADRs** | ≥3 short ADR (options/reasons), ≥1 AI-specific | ADR candidates: auth/JWT; data/pgvector+JSONB; native tools/RAG or prompt policy | reviewed ADRs **TBD** |
| **S5.3 AI_USAGE.md** | AI-generated areas/tool/key prompt/reviewed edits; **≥2 AI code mistakes & fixes**; no unlogged tool use | Team process/documentation | AI_USAGE.md + two actual cases **TBD** |
| **S5.4 Ownership/Git** | Each member owns ≥1 module, all contribute commits, explain low contribution | Team ownership plan | roster/modules/Git history **TBD** |
| **S6 User management** | OAuth2+JWT+RBAC | US-01 | test **TBD** |
| **S6 CRUD** | CRUD main app objects | US-03/05/09/10 | API test **TBD** |
| **S6 Search** | Search/filter/**sort** with **pagination** | US-06; optionally Admin lists | API/UI query tests **TBD** |
| **S6 Realtime/file input** | Realtime/streaming + AI-related file upload | US-11 SSE; US-09 Admin KB upload → RAG | browser + ingestion test **TBD** |
| **S6 AI/dashboard** | A+B+C + Admin number/cost/avg latency/failed/refused | US-09/11/12/15 | end-to-end + metrics test **TBD** |
| **S7 Responsive/persistence** | Desktop/mobile, persistent DB | US-01–15 | responsive & DB reload tests **TBD** |
| **S7 Security/privacy** | No secrets, validate **every API**, owner/tenant scoped RAG, minimize provider payload | US-01/05/09/11/12/13 | 401/403, cross-tenant, injection, secret check **TBD** |
| **S7 Testing** | Core unit tests + AI evaluation | US-03/04/07/11/12 | unit/integration/20-case test logs **TBD** |
| **S8 M1/M2/M3** | M1 proposal + stories/capabilities/architecture sketch; M2 ≥1 AI capability end-to-end + **first 10 eval cases + ≥1 ADR + started AI_USAGE.md**; M3 all S9 | Milestone checklist; dates in assignment placeholders | submission evidence/date **TBD** |
| **S9 Repo/README** | How to run, Docker/deployed link, **env variable names without values**, test accounts | README deliverable | reviewed repo **TBD** |
| **S9 Report** | **PDF 10–20 pages:** stories/trace, architecture, ADRs, AI prompts/tools/RAG, evaluations, cost/latency, limitations | Convert/reuse old report, don't blindly copy full old length | final PDF **TBD** |
| **S9 More deliverables** | `AI_USAGE.md`, slides **≤15**, backup video **3–5 min** | Delivery checklist | files **TBD** |
| **S10 Defense** | ~8min demo with ≥1 guardrail, ~10min **live change**, ~12min individual Q&A; all attend | Demo plan, code-level ownership/traceability | rehearsal **TBD** |
| **S11 Grading risks** | Group 80%, individual 20%; inability to explain can cap grade; missing AI_USAGE penalty; repo-history secrets penalty | ownership/evidence/secret gate | audit before submission **TBD** |

**Do not mark DONE from requirements alone.** Section 3 OAuth2/JWT/streaming, §4 20-eval before/after, §5 AI_USAGE and §6 usage dashboard are major **delivery gaps until verified**.

## 4. Traceability skeleton (fill source/API/test only after code/design mapping)

| US | Legacy source / decision | API endpoint or component | Source code | Test/evidence |
|---|---|---|---|---|
| 01 | UC-01/02/12, FR-S01/02, A01/02; Assignment §3/6 | Auth/Accounts | TBD | TBD |
| 02 | UC-03/04, FR-S03/04 | Profile/Dashboard | TBD | TBD |
| 03 | UC-13, FR-A03/A04 | Assessment Admin | TBD | TBD |
| 04 | UC-05, FR-S05–07, BR-02–04 | Assessment Student | TBD | TBD |
| 05 | UC-09/10, FR-E03–E08, BR-10–12 | Enterprise Scenario | TBD | TBD |
| 06 | UC-06, FR-S08/09, BR-05 | Simulation Catalogue | TBD | TBD |
| 07 | UC-07, FR-S10–13, DR1-02/DR2-02 | Simulation Workspace | TBD | TBD |
| 08 | UC-03/07, FR-S14/17, BR-09/16 | Results / Certificate | TBD | TBD |
| 09 | UC-13 extension, DR1-03, Assignment §4.1.A | Admin KB Studio / Test KB | TBD | TBD |
| 10 | UC-13 extension, approved Admin Builder direction | Agent Builder / Preview | TBD | TBD |
| 11 | UC-08, FR-S15/16, DR1-01/DR2-01 | Student Agent / SSE | TBD | TBD |
| 12 | UC-08 extension, DR1-04/DR2-03 | InsightReport / History | TBD | TBD |
| 13 | UC-08 extension, DR2-04/05 | Conversations | TBD | TBD |
| 14 | UC-08 extension, DR1-04 | ExplorationPlan Confirmation | TBD | TBD |
| 15 | FR-A04 extension, Assignment §6 | AI Usage Admin Dashboard | TBD | TBD |

## 5. ONLY NEW/CHANGED BUSINESS RULES for Chat 2 to check in Core/Agent Class Diagrams v4 + Logical ERD v4

| Rule delta (approved unless stated) | Core v4 impact | AI v4 impact | ERD v4 check (TARGET only) |
|---|---|---|---|
| **D-01 Quiz vs free-form:** Quiz may have correct/incorrect scores; *valid submission*, not a pass threshold, completes quiz. Free-form/practical tasks do **not** require rubric grading; after finishing task show model answer, Student self-compares then proceeds. | Review `SimulationTask`, `TaskDeliverableDefinition`, `ModelAnswer`, `TaskRubric`, `SimulationSubmission.taskScores`; do **not** force all task types to have scores or rubric. | Model-answer gates also apply to all Agent tools and retrieved content. | `TASK_RUBRIC` optional or remove grading-only semantics; `SIMULATION_SUBMISSION.taskScores` quiz-only where applicable; keep unlock/progress. |
| **D-02 Practiced skill is completion evidence:** completing a task records linked **practiced skills**, shown after completing task/program; not auto-awarded proficiency. | `SimulationTask.practicedSkillCodes` is useful; replace `TaskRubric.skillAwardRules` / `awardedSkills(score)` implication. Completion remains separate from scoring. | Agent can discuss skills **practiced**, not declare verified job capability. | Skill result provenance from completed task + pinned scenario version; no dependency on `TASK_RUBRIC` score thresholds. |
| **D-03 Student Agent breadth:** accessible assessment *catalogue and multiple own results*, simulation *catalogue/details and own outcomes*, authorized KB. Before assessment/simulation questions are allowed; do not merge assessment types into unsupported scalar; never reveal locked answers or cross-user/tenant data. | No new Core entity required merely to expose reads; preserve assessment version/participation provenance. | `AgentConfiguration` tool/KB grants + `AgentExecution` actual tool/source evidence; Student audience enforced. | Scoped refs and permissions for Agent read evidence; no unrestricted direct link that bypasses Core authority. |
| **D-04 Admin Agent Builder becomes required:** Admin configure prompt/model, KBs/tools, save immutable version, preview/test, publish, **assign Student**; only assigned Student runs active Agent. | Core identity/role remains authority. | `Agent`, `AgentConfiguration` model needs observable published/assignment behavior; configuration history. | Ensure Agent/config ↔ KB scopes plus audience assignment/publish effective state are representable (table/fields not presupposed). |
| **D-05 Report on explicit request:** AI builds validated structured `InsightReport` **only when Student requests**, owner-only; not enterprise recruitment profile or authoritative score. Plan proposal separate and only becomes `ExplorationPlan` on Student confirmation. | Confirmed plan remains Core-guidance workflow; no changes to objective assessment/completion. | `InsightReport`, `AgentExecution`, `ExplorationPlan` apply; generic `AgentActionProposal` beyond plan is deferred. | Owner, execution/source, schema-versioned report, plan confirmation link; no mandatory Enterprise access/review execution. |
| **D-06 Chat persistence/personalization:** save/resume ordered chat, bounded same-user summary usable later; Student can delete history, deleted-content-only derived summaries no longer used (**deletion propagation PROPOSED**). External AI processing requires consent, ordinary history UX does not ask for an extra consent gate. | Identity/Consent role and purposes remain Core controlled. | `AgentSession.contextSummary`, `InteractionRecord`, `ConsentGrant`, `AgentExecution`; advanced model learning not mandatory. | Avoid enforcing arbitrary 30/90-day expiration from `retentionUntil`; support owner-scoped delete/deactivation. Backup/audit retention still **OPEN**. |
| **D-07 Admin KB upload ≠ student upload:** Admin manages versioned published KB assets; no Student upload-to-chat in MVP; Enterprise-scope KB authorized only if explicitly granted (platform KB first). | Core actor permissions reused; no mandatory Student private file model. | `KnowledgeBase`/`KnowledgeAsset`/`KnowledgeChunk` mandatory; `UserUpload` future-only for Student. | Verify ingestion/version/source/grant relations; avoid making `USER_UPLOAD` mandatory prerequisite for Admin KB source. |

**Non-delta / not requested of Chat 2 now:** physical DDL, Flyway numbering, source refactor, provider framework, generic Agent-to-Agent review design, rewriting all old UML classes, and mapping one domain class to one SQL table. **No diagram edits authorized by this handoff alone.**

## 6. Implementation-readiness gates and carry-forward

- **Requirement baseline ready for design and incremental implementation planning**; Chat 2 must report only diagram/data/contract deltas, reuse as-built modules after actual code audit, protect old Syn APIs and Flyway history. All actual paths/tests stay **TBD** until verified.
- Before code slice involving external AI: verify OAuth2/JWT gap, current consent/security, provider credentials isolation, hidden model-answer access, and prompt/tool boundary. Before production handling personal histories: approve retention/deletion for audit/backup + summary invalidation; policy **OPEN** without blocking code planning.
- Definition of DONE is acceptance tests and assignment evidence, **not** merely matching the TARGET ERD or receiving a successful LLM response.

### Source register

1. `Final_Project_Assignment_2026-2027.docx` — authoritative grading scope, §§1–11.
2. `Updated_CareerSimulation_Report_Requirement_Revised.docx` — existing FR-S01–17, FR-E01–11, FR-A01–04, BR-01–16, NFR-01–10, UC-01–13.
3. `CareerSim_Core_Domain_Model_Simplified.mmd` — TARGET Core v4.
4. `CareerSim_AI_Agent_Domain_Model_Simplified.mmd` — TARGET Agent v4.
5. `CareerSim_Target_Logical_ERD_v4.mmd` — TARGET ERD v4.
6. `CareerSim_AI_Agent_Requirements_Handoff_v4.md` — DISCUSSION DRAFT/TARGET background.
7. User decisions DR1-01–04, DR2-01–05 (including corrections on quiz, model answer, practiced skills, persistent chat and summary memory) — CONFIRMED business baseline; no code evidence claimed.
