# CareerSim — Báo cáo triển khai Package A

Ngày: 14/10/2026. Implementer: Codex, theo phê duyệt trực tiếp của project lead.
Branch: `codex/package-a-core`. HEAD: `f00bd55ddacca3b4f0efa6f17d07de45a2acd0e7`.
Đây là thay đổi chưa commit trên HEAD trên; SHA không đại diện cho toàn bộ diff.
Kết luận: **Package A đã được triển khai và vượt qua các cổng kỹ thuật trên môi trường cô lập. Chưa cutover/deploy.**

## 1. Phạm vi, nguồn và giới hạn traceability

Đã đọc chuỗi `.agent` bắt buộc, toàn bộ năm tài liệu Astra trong
`docs/architecture/data-model/physical-design-draft/`, cùng bốn ERD/UML/design notes
được cung cấp. Phạm vi thực hiện: Stages 0–7, Flyway V13–V16, tích hợp frontend tối
thiểu và giữ Syn hiện tại. Trạng thái PROPOSED trong năm file gốc được giữ nguyên;
quyền triển khai đến từ phê duyệt rõ ràng của người dùng trong chat.

Revised SRS và `how-to-design-class.pdf` không có để đối chiếu: **NOT VERIFIED**.
Không suy ra FR/UC mới từ tài liệu chưa được cung cấp. Trace dùng R-ID/R-AS/R-SIM/
R-EV/R-PR/R-MIG và AC của tài liệu 04, không tuyên bố đã hoàn tất acceptance của SRS.

Package B, Agent Platform mới, V17–V19, migration dữ liệu thật, runtime inventory
của database hiện hữu, production cutover, commit/push/deploy đều chưa thực hiện.
Không chạy ứng dụng với datasource mặc định hoặc chạy `docker compose up` của dự án.

## 2. Hiện trạng Git và bảo vệ dữ liệu

Branch ban đầu: `codex/syn-core-vector-rag`; tạo branch triển khai trên cùng HEAD.
Bốn tài liệu design đã staged của người dùng vẫn staged; năm tài liệu Astra vốn
untracked vẫn được giữ. So sánh SHA-256 với `package-a-baseline.json`: **21/21 file
được bảo vệ không đổi**, gồm V1–V12 và chín tài liệu design/specification.

Database hiện hữu: **NOT ACCESSED / NOT VERIFIED**. Mọi migration, test SQL,
trigger gây lỗi có chủ đích, tạo role, dump/restore đều giới hạn trong database mới
do Testcontainers tạo. Không chuyển đổi consent timestamp, điểm cũ hay lượt làm cũ
thành consent grant, version lịch sử, enrollment, final submission hoặc completion.

## 3. Các stage và chức năng hoàn thành

| Stage | Chức năng và trách nhiệm thực tế | Bằng chứng chính |
|---|---|---|
| 0 | Baseline branch/SHA/hash, inventory legacy, acceptance skeleton | `.agent/reports/package-a-baseline.json`; `SchemaUpgradeIntegrationTest.java:71` |
| 1 | Schema additive, composite FK/index/guard, importer review-manifest và ledger | `MigrationPreflightService.java:31`; `CoreReviewedImportTarget.java:31`; V13–V16 |
| 2 | Đăng ký Student hoặc Enterprise; profile/org/membership atomic; session/CSRF; trạng thái/role hiện hành; quyền publish riêng; consent theo purpose/recipient | `IdentityServiceImpl.java:28,50`; `ConsentServiceImpl.java:48,85`; `CorePackageAIntegrationTest.java:426,1406,1674` |
| 3 | AssessmentType/Version; evaluator `riasec-sum/1`; attempt pin version; retake UUID; draft; submit atomic; result bất biến và replay | `VersionedAssessmentServiceImpl.java:73,111,147`; `CorePoliciesTest.java:72`; core tests `:1451,1517` |
| 4 | Enterprise authoring, clone ID mới, revision toàn graph, publish reviewed content, resource/answer/skill tách riêng, private assets | `ScenarioAuthoringServiceImpl.java:148`; core tests `:240,303,359` |
| 5 | Enrollment duy nhất mỗi student/version; V1/V2 song song; TEXT/URL/FILE/JSON; draft If-Match; FINAL UUID/hash, không nộp lại | `EnrollmentServiceImpl.java:32`; `TaskSubmissionServiceImpl.java:95`; core tests `:755,1532,1600,1637` |
| 6 | Model answer sau FINAL; completion trong transaction nộp required task cuối; certificate snapshot/HTML và snippet cố định | `CompletionServiceImpl.java:29`; core tests `:359,946,1532` |
| 7 | Dashboard typed evidence, FINAL-task practice kể cả chưa completion; plan confirmation/history; v1 compatibility; session/CSRF/core React và Syn | `GuidanceEvidenceQueryImpl.java:25`; `ExplorationPlanServiceImpl.java:63`; core tests `:490,755,1719`; frontend component tests |

Các đường dẫn Java chính trong bảng nằm dưới
`backend/src/main/java/edu/hcmute/careersim/` theo module tương ứng. Các test core
nằm dưới `backend/src/test/java/edu/hcmute/careersim/system/migration/`.

Không có assessment prerequisite cho enrollment. Không có score/automatic grading
trong luồng simulation mới. Một final submission là một tập deliverable được chấp
nhận nguyên khối; optional item có mặt vẫn phải hợp lệ. Optional task nộp sau
completion không thay đổi snapshot/snippet. Certificate chỉ xác nhận hoàn thành
mô phỏng, không chứng nhận năng lực chuyên môn.

## 4. Kiến trúc và sequence đã triển khai

Các module thực tế: `identity`, `enterprise`, `assessment`, `simulation`, `catalog`,
`guidance`, `common`, `system`. Chưa tạo module `agent`/`knowledge` mới của Package B.
Controller chuyển HTTP/principal/DTO vào facade; service điều phối quyền, lock,
transaction và lifecycle; DAO thực hiện SQL; evaluator/validator/artifact factory
giữ các quy tắc tương ứng. Public ports dùng immutable records, không expose JPA graph.

Persistence mới dùng JDBC records và schema-qualified SQL. Chín JPA mapping legacy
được giữ với Hibernate `validate`; không tạo hai representation mutable cho cùng
aggregate. Đây là lựa chọn được cho phép bởi tài liệu 03 §1, không thay đổi physical
schema. Hai service v1 cũ còn dependency vào catalog được nêu rõ trong ArchUnit;
các module mới bị chặn dependency ngược và truy cập DAO/implementation module khác.

```mermaid
sequenceDiagram
    actor Student
    participant UI as React + session/CSRF
    participant API as Controller
    participant Service as Owning facade/ServiceImpl
    participant Policy as Evaluator/DeliverableValidator
    participant DB as PostgreSQL 17
    Student->>UI: Lưu hoặc xác nhận nộp task
    UI->>API: DTO + cookie + X-CSRF-TOKEN + If-Match
    API->>Service: Actor ID đáng tin cậy + command
    Service->>DB: Kiểm tra current actor; lock user → enrollment → submission
    Service->>Policy: Validate immutable definitions + canonical hash
    Service->>DB: Persist items → FINAL
    Service->>DB: Đủ required tasks? Insert completion + snapshot + audit
    DB-->>Service: Commit nguyên khối
    Service-->>UI: Persisted submission/completion IDs
    Student->>API: GET model answer / certificate / snippet
    API->>Service: Kiểm tra owner và release policy
```

Topology hiện tại và hình kiến trúc được cập nhật trong `docs/architecture/README.md`.
Compose có gateway, React, Spring Boot, PostgreSQL và volume private assets. Smoke
thực tế dùng Nginx/PG17 container mới cùng Spring Boot ở random host port; đây không
phải bằng chứng đã triển khai toàn bộ Compose lên VM hay production.

## 5. Migrations và inventory database

| Migration | Các bảng mới |
|---|---|
| V13 | `enterprise`, `enterprise_representative`, `consent_grant`, `audit_event`, `migration_record`; thêm profile/account fields và normalized-email uniqueness |
| V14 | `file_asset`, `assessment_type`, `assessment_version`, `assessment_question`, `assessment_attempt`, `assessment_response`, `assessment_result` |
| V15 | `scenario`, `scenario_version`, `simulation_task`, `task_deliverable_definition`, `task_resource`, `model_answer`, `career_skill`, `task_skill`, `simulation_enrollment`, `task_submission`, `submitted_deliverable`, `simulation_completion` |
| V16 | `exploration_plan`; JSON shape checks cho retained career-path dictionary |

Tổng cộng **25 bảng mới, 46 bảng application khi coexistence** với 21 bảng legacy,
không tính Flyway history. `source_proposal_id` ở plan để nullable và unique; FK
proposal chỉ được thêm trong Package B theo thiết kế, chưa tạo FK tới bảng không tồn tại.

Composite FK bảo vệ attempt/question/version, enrollment/task/version và
submission/definition/task. Parent-lock triggers chặn sửa published graph và final
children. Result, submitted attempt và completion có guard bất biến. Index và
constraint mới có tên phân biệt; normalized-email collision làm V13 rollback với
`CORE_EMAIL_COLLISION`, không merge hoặc repair tài khoản.

Bằng chứng trên database cô lập:

- Fresh V1–V16 và Hibernate validate chạy thành công trong Spring integration test.
- V12 có demo/history → V16: fingerprint của đủ 21 bảng nguồn không đổi (loại các
  cột mới khi so sánh); inventory 46; không seed consent/completion mới; chạy lại
  Flyway không thực thi migration bổ sung.
- Database cô lập có email collision: V13 thất bại, hai nguồn vẫn còn và bảng
  enterprise chưa được tạo (`SchemaUpgradeIntegrationTest.java:17`).
- Raw SQL sai composite FK, sửa published children/final deliverables/result/
  completion bị từ chối. Barrier tests chứng minh child edit/publication và các
  finalizer không bỏ qua parent lock.
- `pg_dump -Fc` và `pg_restore` sang `package_a_restored` mới: head V16,
  final submission/snippet/FK còn đúng; đọc và tạo attempt mới thành công.
- Role runtime thử nghiệm có DML/USAGE nhưng `CREATE TABLE` bị từ chối SQLSTATE
  `42501` (`CorePackageAIntegrationTest.java:55`). Chưa provision role trên DB thật.

Importer mặc định dry-run, tối đa 100 entry/manifest, một entry/transaction để retry
có giới hạn. Kiểm tra database/schema/head, reviewer ADMIN, source hash, mapping
hash và readback. Cùng source/mapping trả UNCHANGED; source/mapping thay đổi trả
CONFLICT, không ghi đè target. Thử gây lỗi ở ledger insert chứng minh target và
ledger cùng rollback, sau đó retry chỉ tạo một bản sao (`core test :83`).

Importer chỉ tạo assessment/scenario draft hiện tại đã review và plan có provenance
xác nhận. Snapshot câu hỏi đối chiếu prompt/dimension/order nguồn. Scenario cần
enterprise mapping tường minh. Plan cần đúng owner/nội dung/source hash và thời
điểm xác nhận từ evidence riêng; không dùng `updated_at` thay cho `confirmed_at`.
Reference provenance do người review chịu trách nhiệm xác minh; tooling kiểm tra
binding và quyền, không chứng minh tính xác thực của tài liệu bên ngoài. Chưa chạy
import nội dung thật hoặc sao chép historical attempts/consent/completion.

### SHA-256 các migration mới

| File | SHA-256 |
|---|---|
| V13__identity_enterprise_consent_foundation.sql | A40519A50BA84B8522F3BAA09B07C4FD8BCDB25F2B92A0FCA31CFEDE2E0B8A5D |
| V14__versioned_assessment_and_assets.sql | 46881035DD5314793A38BF06756BE7721B1FBFD201DE855F1906D3C9CF5FECF9 |
| V15__versioned_simulation_and_completion.sql | 2701896370EA20C0242DC0E989E72B37467EE87673943EA4AB7F803312C34341 |
| V16__confirmed_exploration_plan.sql | 1AAF5C915686E0EFE2B91FFFC782E04F2C2BBA9EE0B8F548EC31C670D72BF68A |

## 6. Security, consent và giữ Syn

- Role domain ADMIN map đúng storage ADMINISTRATOR; không thêm role thứ tư. Không
  self-register admin hoặc self-join một enterprise có sẵn. Admin quản lý membership
  của account đã được provision; transfer tenant yêu cầu review riêng.
- Session fixation protection, HttpOnly/SameSite=Lax, CSRF cả login/logout và refresh
  token sau login/logout. Cookie Secure mặc định true; test HTTP cô lập dùng false.
- Password mới dùng DelegatingPasswordEncoder/PBKDF2 v5.8, policy 12–128 ký tự;
  hỗ trợ bcrypt có prefix hợp lệ, từ chối demo/malformed hashes. Không cấp demo login
  production. LocalDemo filter chỉ hoạt động khi bật flag và profile local-demo.
- Mỗi protected use case kiểm tra actor/status/role hiện tại. Header X-Student-Id
  không xác thực người dùng; header khác owner bị từ chối ở v1.
- Sharing PROFILE/ASSESSMENT/COMPLETION/SUBMISSION tách riêng; submission còn phải
  thuộc scenario của enterprise nhận. Admin không mặc nhiên xem private evidence.
- External AI theo từng provider, không suy từ timestamp cũ hoặc enterprise sharing.
  Explicit confirmed/current-policy mới tạo grant; revoke tăng privacy version,
  cancel/redact protected operational Syn payload và chặn dispatch/replay tiếp theo.
- Per-user admission lock dùng chung cho grant/revoke và bắt đầu outbound request;
  không giữ lock/transaction khi chờ remote response. Barrier test ở core `:1020`.
  Finish/replay revalidate; output tới muộn không được phục hồi sau revoke.
- Private assets 1 byte–10 MiB, MIME/signature/size/checksum, random storage key,
  path/symlink bounds; download attachment + no-store, kiểm tra association và release.
  Không expose storage key hoặc tải nội dung từ URL bên ngoài.

Giữ provider adapters, Syn lifecycle/transport/retry, deterministic Standard policy,
legacy knowledge/vector infrastructure và EN/VI. Chỉ bổ sung context mới từ FINAL
task/skill, reference SUBMISSION có version và kiểm tra lại quyền; không nhét
completion vào score card. Confirmation plan v1 nhận session ID để so khớp draft
server; direct confirmation được lưu ở plan mới và archive plan trước.

Chưa triển khai registry/configurable multi-agent, KB mới, memory dài hạn, native
Agent loop mới, proposals/approval execution hoặc migrations của Package B.

## 7. API compatibility và frontend

Tất cả route dưới context `/api`. New IDs là decimal strings; revision truyền qua
If-Match. Envelope ApiResponse/ErrorResponse được giữ. 401/403 cho auth/CSRF, 404
resource ngoài ownership, 409 conflict/final lock, 422 validation, 428 thiếu revision.

| Nhóm | API thực tế |
|---|---|
| Identity | `/v2/auth/{csrf,register,login,logout}`, `/v2/me`, `/v2/me/profile`, `/v2/me/consents`, `/{grantId}/revoke` |
| Enterprise/admin | `/v2/enterprises/me`, `/v2/admin/enterprises/{id}/{verification,publishing}`, `/representatives/{userId}`, `/v2/admin/accounts/{id}/status` |
| Assessment | `/v2/assessment-types`, `/v2/assessment-versions/{id}/attempts`, `/v2/assessment-attempts/{id}` + `/responses`, `/submit`, `/result`; admin type/version/publication routes |
| Content/catalog | `/v2/scenarios`, `/v2/scenarios/{slug}`, `/v2/enterprise/scenarios`, root `/versions`, `/availability`; `/v2/enterprise/scenario-versions/{id}` + `/publish`; career-skills routes |
| Participation | `/v2/scenario-versions/{id}/enrollments`, `/v2/enrollments`, `/v2/enrollments/{id}`, task `/submission` + `/finalize`, `/model-answer` |
| Evidence/files | `/v2/completions/{id}` + `/certificate`; `/v2/files` và `/{id}/download?useType=...&useId=...`; enterprise scoped profile/assessment/completion/submission reads |
| Guidance | `/v2/guidance/dashboard`, `/v2/plans`, `/v2/plans/confirm`; giữ `/v1/guidance` Syn recovery/cancel/source/plan APIs |

V1 historical reads và numerical result contracts được giữ trong namespace legacy.
Legacy assessment/graded simulation writes mặc định 409 WORKFLOW_UPGRADE_REQUIRED;
không dual-write hay chuyển graded attempt thành final work. Core và legacy write
không thể bật cùng lúc. V2 catalog và frontend core có flag phối hợp, mặc định off;
v2 content ban đầu rỗng cho tới khi có publication review hợp lệ.

Frontend thêm Account/consent, assessment pin version/UUID, enrollment/deliverables,
private upload, final lock/answers/completion, typed dashboard và confirm plan.
API client quản lý session-CSRF. Local drafts assessment/task dùng key theo owner
và attempt/enrollment/task; bản nháp legacy vẫn còn; lỗi lưu không tạo điểm/kết quả
giả. Nút demo assessment không hiện trong core; mock legacy chỉ cho dev bật rõ ràng.
Catalog landing/main cùng dùng v2 khi flag bật; giữ translation version và EN/VI.
Native labels/fieldset/keyboard controls và reduced-motion scroll được giữ.
Syn bỏ reply tới muộn sau account change; không tái hiển thị private transcript cũ.

Deployment files thêm build flag core, persistent private storage, upload limit,
runtime/migrator credential variables. CI frontend build được chuyển thành artifact
đầu vào cho backend gateway test. Workflow GitHub chưa được chạy remote.

## 8. Kết quả kiểm chứng thực tế

| Check | Lệnh/scenario đã chạy | Kết quả |
|---|---|---|
| Backend cuối | `.\mvnw.cmd -q verify` từ backend | **PASS: 186 discovered, 182 passed, 0 failed/errors, 4 skipped**; compile, tests, Spotless, JAR, JaCoCo |
| Frontend cuối | PowerShell `$env:VITE_CORE_V2_ENABLED='true'; npm run check` từ frontend | **PASS: 65 tests / 12 files**, ESLint và Vite production build |
| Compose | `docker compose config --quiet` | PASS; không khởi động default stack |
| Whitespace | `git diff --check` | PASS trên thay đổi triển khai |
| Protected bytes | SHA-256 đối chiếu baseline | PASS 21/21; staging design files không đổi |
| DB migration | SchemaUpgradeIntegrationTest + core PG17 tests | PASS fresh/upgrade/rerun/collision/FK/guards/history preservation |
| Gateway/restore | Core integration `realGatewayNoAiWorkflowAndIsolatedBackupRestore` | PASS HTTP gateway + compiled frontend document + core API + database readback + isolated dump/restore |
| AI/fallback | Existing mocked provider suite + no-AI Vietnamese Syn smoke | PASS; không gọi live AI provider |

Các nhóm test quan trọng: CSRF/session rotation và current status; normalized email;
cross-owner/tenant; retake/replay; wrong/duplicate/missing/out-of-range answer;
min/max và tie order; canonical Unicode/LF/hash; published child guards và race;
bốn payload formats/optional validation; parallel enrollment/assessment/finalizers;
rollback assessment/completion/import; fixed snapshot; answer attachment release;
purpose/provider consent và revoke admission; plan archive; legacy deprecation;
frontend unsaved/locked/empty/VI/account-change states.

Bốn skip thuộc các opt-in live tests trong GeminiEmbeddingProviderTest (2),
GeminiSynGuidanceLiveTest và OpenAiSynGuidanceLiveTest. Không coi skip là pass/live
quality evidence. Các lần kiểm tra ban đầu từng lỗi fixture profile/consent cũ,
CSRF mock và explicit JSON-null DTO; đã sửa nguyên nhân, không xóa hoặc giảm
assertion để làm xanh. Hai legacy dependency exceptions trong architecture test
được giữ tường minh theo compatibility boundary đã duyệt.

## 9. Acceptance và những gì còn gated

| AC của tài liệu 04 | Trạng thái Package A |
|---|---|
| AC01–04 | Code + isolated security/ownership/enterprise/purpose-sharing tests PASS |
| AC05–08 | Pinned assessment/retake/membership/FK/scoring/replay/rollback tests PASS |
| AC09–16 | Version/content guards, enrollment, formats, final lock, release/FK/concurrency tests PASS |
| AC17–19 | No-AI completion/fixed artifacts/final-task practice including incomplete enrollment PASS |
| AC26 phần legacy Syn | Explicit provider consent, old timestamp denied, revoke ordering/late payload tests PASS |
| AC33 | Legacy read/Syn contracts retained, write upgrade gate + coordinated React PASS |
| AC34–35 phần core | V1–V16 fresh/upgrade/preservation, resumable import and rollback PASS |
| AC36 | Isolated collision/preflight behavior PASS; valuable-runtime inventory **NOT VERIFIED** |
| AC37 | Isolated dump/restore PASS; valuable-data backup/rehearsal chưa được duyệt |
| AC38 | Automated gateway core API/readback + component EN/VI/accessibility states PASS; chưa có human browser/product UAT hay VM deployment |
| AC20–25, AC27–32, AC39 mới | Package B hoặc human/live corpus acceptance; chưa triển khai/chưa xác minh |

Các giới hạn và rủi ro còn lại:

1. Revised SRS thiếu; human semantic review/UAT, giấy phép/content ownership thực,
   corpus quality và live AI quality chưa xác minh. Không gán lịch sử cho dữ liệu thiếu chứng cứ.
2. Production runtime head/checksum/anomalies/backup/roles/TLS/storage permissions
   chưa kiểm tra. Compose development credential fallback không provision role
   production. Phải dùng DML-only app role và migrator riêng khi rollout được duyệt.
3. V2 publication cần nội dung được review và principal có quyền hợp lệ. Không
   tự tạo admin, enterprise ownership hay current-content publication từ seed cũ.
4. Session/admission coordination hiện cho một instance; restart đăng nhập lại.
   Cần thiết kế lại coordination trước horizontal scaling. Không claim distributed guarantee.
5. Asset bytes có thể thành orphan khi metadata transaction thất bại; private,
   không được attach READY. Cleanup/retention tự động chưa được duyệt, chưa thực hiện.
6. HTML certificate/private snapshot đã có; PDF/signing/public verification ngoài scope.
   New assessments khác RIASEC cần evaluator/schema được đăng ký, không tự dùng SUM.
7. Full Compose image build/VM deployment và thao tác trình duyệt thủ công chưa chạy.
   Gateway smoke không được trình bày thành production hoặc human usability evidence.

Flags chỉ chặn route/write authority, không tắt Flyway: startup vẫn có thể áp dụng V13–V16. Startup với database có giá trị cũng thuộc runtime approval gate.

## 10. Điều kiện vào Package B và tái lập

Package B có thể dùng ActorAccess, EnterpriseAccess/registration/recipient SPI,
ConsentAccess/ConsentRevocationListener/PrivacyAdmission, FileAssetService,
AuditWriter, assessment/participation evidence ports, ScenarioAuthoringService và
PlanCommands. Phải bổ sung registry/config versions/KB grants/runtime/memory/
approval contracts cùng V17–V19 theo specification; không reuse legacy numeric K/A/S
IDs thành new IDs và không nâng completion thành grade. Chọn runtime/corpus/grants
thật chỉ sau các approval riêng; giữ regression suite này khi thêm adapters mới.

Để tái lập kiểm tra trên máy có Java 17/Node 22/Docker và dependencies đã chuẩn bị:
build/check frontend với core flag trước; gateway test cần `frontend/dist`. Sau đó
chạy Maven verify và Compose config. CI đã download đúng build artifact trước verify.
Maven PG/gateway tests tự tạo database/container mới; không cần datasource dự án.
Không chạy default application startup hoặc migration/import trên DB hiện hữu để
thay thế bước approval runtime.

Danh sách file thay đổi ở phụ lục bên dưới, loại bốn staged design files và năm
specifications vốn có của người dùng. Không commit, push hoặc deploy.

## Phụ lục — File/module thay đổi

Snapshot gồm 169 file triển khai/tài liệu (không tính build outputs).

```text
.agent/plans/package-a-core.md
.agent/reports/ai/2026-10-14-package-a.md
.agent/reports/package-a-baseline.json
.agent/reports/package-a-core-verification.vi.md
.agent/reports/package-a-test-summary.json
.agent/TASKS.md
.env.example
.github/workflows/ci.yml
.gitignore
backend/Dockerfile
backend/src/main/java/edu/hcmute/careersim/assessment/access/SharedAssessmentAccess.java
backend/src/main/java/edu/hcmute/careersim/assessment/access/VersionedAssessmentAccess.java
backend/src/main/java/edu/hcmute/careersim/assessment/controller/AssessmentController.java
backend/src/main/java/edu/hcmute/careersim/assessment/controller/SharedAssessmentController.java
backend/src/main/java/edu/hcmute/careersim/assessment/controller/VersionedAssessmentController.java
backend/src/main/java/edu/hcmute/careersim/assessment/dao/VersionedAssessmentDao.java
backend/src/main/java/edu/hcmute/careersim/assessment/domain/RiasecV1Evaluator.java
backend/src/main/java/edu/hcmute/careersim/assessment/dto/VersionedAssessmentContracts.java
backend/src/main/java/edu/hcmute/careersim/assessment/service/AssessmentAuthoringService.java
backend/src/main/java/edu/hcmute/careersim/assessment/service/impl/SharedAssessmentService.java
backend/src/main/java/edu/hcmute/careersim/assessment/service/impl/VersionedAssessmentServiceImpl.java
backend/src/main/java/edu/hcmute/careersim/assessment/service/VersionedAssessmentService.java
backend/src/main/java/edu/hcmute/careersim/catalog/controller/VersionedCatalogController.java
backend/src/main/java/edu/hcmute/careersim/catalog/service/impl/VersionedCatalogServiceImpl.java
backend/src/main/java/edu/hcmute/careersim/catalog/service/VersionedCatalogService.java
backend/src/main/java/edu/hcmute/careersim/common/audit/AuditWriter.java
backend/src/main/java/edu/hcmute/careersim/common/config/CoreRolloutConfiguration.java
backend/src/main/java/edu/hcmute/careersim/common/config/LocalDemoAuthenticationFilter.java
backend/src/main/java/edu/hcmute/careersim/common/config/SecurityConfig.java
backend/src/main/java/edu/hcmute/careersim/common/exception/DomainValidationException.java
backend/src/main/java/edu/hcmute/careersim/common/exception/ErrorCode.java
backend/src/main/java/edu/hcmute/careersim/common/exception/GlobalExceptionHandler.java
backend/src/main/java/edu/hcmute/careersim/common/storage/FileAssetController.java
backend/src/main/java/edu/hcmute/careersim/common/storage/FileAssetDao.java
backend/src/main/java/edu/hcmute/careersim/common/storage/FileAssetService.java
backend/src/main/java/edu/hcmute/careersim/common/storage/FileUploadContext.java
backend/src/main/java/edu/hcmute/careersim/common/storage/FileUseAuthorizer.java
backend/src/main/java/edu/hcmute/careersim/common/storage/LocalFileAssetService.java
backend/src/main/java/edu/hcmute/careersim/common/storage/PrivateFileStorage.java
backend/src/main/java/edu/hcmute/careersim/common/value/CoreValues.java
backend/src/main/java/edu/hcmute/careersim/enterprise/access/EnterpriseAccess.java
backend/src/main/java/edu/hcmute/careersim/enterprise/access/EnterpriseDirectoryAccess.java
backend/src/main/java/edu/hcmute/careersim/enterprise/adapter/EnterpriseFileUploadContext.java
backend/src/main/java/edu/hcmute/careersim/enterprise/adapter/EnterpriseSharingRecipient.java
backend/src/main/java/edu/hcmute/careersim/enterprise/adapter/JdbcEnterpriseDirectoryAccess.java
backend/src/main/java/edu/hcmute/careersim/enterprise/controller/EnterpriseController.java
backend/src/main/java/edu/hcmute/careersim/enterprise/dao/EnterpriseDao.java
backend/src/main/java/edu/hcmute/careersim/enterprise/service/EnterpriseService.java
backend/src/main/java/edu/hcmute/careersim/enterprise/service/impl/EnterpriseServiceImpl.java
backend/src/main/java/edu/hcmute/careersim/guidance/adapter/LegacySynConsentRevocation.java
backend/src/main/java/edu/hcmute/careersim/guidance/controller/CoreGuidanceController.java
backend/src/main/java/edu/hcmute/careersim/guidance/controller/GuidanceController.java
backend/src/main/java/edu/hcmute/careersim/guidance/dao/ConfirmedPlanDao.java
backend/src/main/java/edu/hcmute/careersim/guidance/dao/JdbcSynTurnDao.java
backend/src/main/java/edu/hcmute/careersim/guidance/dao/SynTurnDao.java
backend/src/main/java/edu/hcmute/careersim/guidance/dto/PlanContracts.java
backend/src/main/java/edu/hcmute/careersim/guidance/dto/SavePlanRequest.java
backend/src/main/java/edu/hcmute/careersim/guidance/infrastructure/GeminiEmbeddingProvider.java
backend/src/main/java/edu/hcmute/careersim/guidance/infrastructure/GeminiSynGuidanceProvider.java
backend/src/main/java/edu/hcmute/careersim/guidance/infrastructure/GeminiWebSearchProvider.java
backend/src/main/java/edu/hcmute/careersim/guidance/infrastructure/OpenAiSynGuidanceProvider.java
backend/src/main/java/edu/hcmute/careersim/guidance/infrastructure/SynHttpRequests.java
backend/src/main/java/edu/hcmute/careersim/guidance/service/ExplorationPlanService.java
backend/src/main/java/edu/hcmute/careersim/guidance/service/GuidanceEvidenceQuery.java
backend/src/main/java/edu/hcmute/careersim/guidance/service/impl/ExplorationPlanServiceImpl.java
backend/src/main/java/edu/hcmute/careersim/guidance/service/impl/GuidanceEvidenceQueryImpl.java
backend/src/main/java/edu/hcmute/careersim/guidance/service/impl/GuidanceServiceImpl.java
backend/src/main/java/edu/hcmute/careersim/guidance/service/PlanCommands.java
backend/src/main/java/edu/hcmute/careersim/guidance/service/SynAgentToolbox.java
backend/src/main/java/edu/hcmute/careersim/guidance/service/SynExecutionScope.java
backend/src/main/java/edu/hcmute/careersim/guidance/service/SynTurnLifecycleService.java
backend/src/main/java/edu/hcmute/careersim/identity/access/AccountPrincipal.java
backend/src/main/java/edu/hcmute/careersim/identity/access/ActorAccess.java
backend/src/main/java/edu/hcmute/careersim/identity/access/ConsentAccess.java
backend/src/main/java/edu/hcmute/careersim/identity/access/ConsentRevocationListener.java
backend/src/main/java/edu/hcmute/careersim/identity/access/EnterpriseRegistration.java
backend/src/main/java/edu/hcmute/careersim/identity/access/JdbcActorAccess.java
backend/src/main/java/edu/hcmute/careersim/identity/access/JdbcConsentAccess.java
backend/src/main/java/edu/hcmute/careersim/identity/access/JdbcStudentAccountAccess.java
backend/src/main/java/edu/hcmute/careersim/identity/access/PrivacyAdmission.java
backend/src/main/java/edu/hcmute/careersim/identity/access/SharedProfileAccess.java
backend/src/main/java/edu/hcmute/careersim/identity/access/SharingRecipientAccess.java
backend/src/main/java/edu/hcmute/careersim/identity/access/StudentAccountAccess.java
backend/src/main/java/edu/hcmute/careersim/identity/controller/IdentityController.java
backend/src/main/java/edu/hcmute/careersim/identity/controller/SharedProfileController.java
backend/src/main/java/edu/hcmute/careersim/identity/dao/IdentityDao.java
backend/src/main/java/edu/hcmute/careersim/identity/dto/IdentityContracts.java
backend/src/main/java/edu/hcmute/careersim/identity/service/ConsentService.java
backend/src/main/java/edu/hcmute/careersim/identity/service/IdentityService.java
backend/src/main/java/edu/hcmute/careersim/identity/service/impl/ConsentServiceImpl.java
backend/src/main/java/edu/hcmute/careersim/identity/service/impl/IdentityServiceImpl.java
backend/src/main/java/edu/hcmute/careersim/identity/service/impl/SharedProfileService.java
backend/src/main/java/edu/hcmute/careersim/simulation/access/ParticipationEvidenceAccess.java
backend/src/main/java/edu/hcmute/careersim/simulation/access/ScenarioCatalogAccess.java
backend/src/main/java/edu/hcmute/careersim/simulation/access/SharedSimulationAccess.java
backend/src/main/java/edu/hcmute/careersim/simulation/controller/EnrollmentController.java
backend/src/main/java/edu/hcmute/careersim/simulation/controller/ScenarioAuthoringController.java
backend/src/main/java/edu/hcmute/careersim/simulation/controller/SharedSimulationController.java
backend/src/main/java/edu/hcmute/careersim/simulation/controller/SimulationAttemptController.java
backend/src/main/java/edu/hcmute/careersim/simulation/dao/CompletionDao.java
backend/src/main/java/edu/hcmute/careersim/simulation/dao/EnrollmentDao.java
backend/src/main/java/edu/hcmute/careersim/simulation/dao/ScenarioDao.java
backend/src/main/java/edu/hcmute/careersim/simulation/dao/SimulationAccessDao.java
backend/src/main/java/edu/hcmute/careersim/simulation/domain/CompletionArtifacts.java
backend/src/main/java/edu/hcmute/careersim/simulation/domain/DeliverableValidator.java
backend/src/main/java/edu/hcmute/careersim/simulation/dto/ScenarioContracts.java
backend/src/main/java/edu/hcmute/careersim/simulation/service/CompletionService.java
backend/src/main/java/edu/hcmute/careersim/simulation/service/EnrollmentService.java
backend/src/main/java/edu/hcmute/careersim/simulation/service/impl/CompletionServiceImpl.java
backend/src/main/java/edu/hcmute/careersim/simulation/service/impl/EnrollmentServiceImpl.java
backend/src/main/java/edu/hcmute/careersim/simulation/service/impl/ScenarioAuthoringServiceImpl.java
backend/src/main/java/edu/hcmute/careersim/simulation/service/impl/SharedSimulationService.java
backend/src/main/java/edu/hcmute/careersim/simulation/service/impl/TaskSubmissionServiceImpl.java
backend/src/main/java/edu/hcmute/careersim/simulation/service/ModelAnswerAccess.java
backend/src/main/java/edu/hcmute/careersim/simulation/service/ScenarioAuthoringService.java
backend/src/main/java/edu/hcmute/careersim/simulation/service/TaskSubmissionService.java
backend/src/main/java/edu/hcmute/careersim/system/migration/CoreReviewedImportTarget.java
backend/src/main/java/edu/hcmute/careersim/system/migration/LegacyBackfillCommand.java
backend/src/main/java/edu/hcmute/careersim/system/migration/MigrationContracts.java
backend/src/main/java/edu/hcmute/careersim/system/migration/MigrationPreflightService.java
backend/src/main/java/edu/hcmute/careersim/system/migration/MigrationRecordDao.java
backend/src/main/java/edu/hcmute/careersim/system/migration/ReviewedImportTarget.java
backend/src/main/resources/application.yml
backend/src/main/resources/db/migration/V13__identity_enterprise_consent_foundation.sql
backend/src/main/resources/db/migration/V14__versioned_assessment_and_assets.sql
backend/src/main/resources/db/migration/V15__versioned_simulation_and_completion.sql
backend/src/main/resources/db/migration/V16__confirmed_exploration_plan.sql
backend/src/test/java/edu/hcmute/careersim/architecture/LayerDependencyTest.java
backend/src/test/java/edu/hcmute/careersim/common/config/LocalDemoAuthenticationFilterTest.java
backend/src/test/java/edu/hcmute/careersim/guidance/controller/GuidanceControllerTest.java
backend/src/test/java/edu/hcmute/careersim/guidance/dao/GuidancePersistenceIntegrationTest.java
backend/src/test/java/edu/hcmute/careersim/guidance/service/GuidanceServiceTest.java
backend/src/test/java/edu/hcmute/careersim/simulation/controller/SimulationAttemptLocaleControllerTest.java
backend/src/test/java/edu/hcmute/careersim/system/migration/CorePackageAIntegrationTest.java
backend/src/test/java/edu/hcmute/careersim/system/migration/CorePoliciesTest.java
backend/src/test/java/edu/hcmute/careersim/system/migration/SchemaUpgradeIntegrationTest.java
deployment/nginx/default.conf
docker-compose.yml
docs/architecture/README.md
frontend/Dockerfile
frontend/nginx.conf
frontend/src/app/App.jsx
frontend/src/features/assessment/components/AssessmentForm.jsx
frontend/src/features/assessment/components/AssessmentResult.jsx
frontend/src/features/assessment/hooks/useAssessment.js
frontend/src/features/assessment/pages/VersionedAssessmentPage.jsx
frontend/src/features/guidance/api/guidanceApi.test.js
frontend/src/features/guidance/components/SynWidget.jsx
frontend/src/features/guidance/components/SynWidget.test.jsx
frontend/src/features/guidance/pages/CoreDashboardPage.jsx
frontend/src/features/identity/AccountPage.jsx
frontend/src/features/simulations/api/simulationApi.js
frontend/src/features/simulations/api/simulationCatalogApi.js
frontend/src/features/simulations/api/simulationCatalogApi.test.js
frontend/src/features/simulations/components/SimulationCard.jsx
frontend/src/features/simulations/components/SimulationCatalog.jsx
frontend/src/features/simulations/hooks/useSimulationAttempt.js
frontend/src/features/simulations/pages/CoreSimulationPage.jsx
frontend/src/features/simulations/pages/SimulationAttemptPage.jsx
frontend/src/features/simulations/pages/SimulationCatalogPage.jsx
frontend/src/features/simulations/pages/SimulationDetailPage.jsx
frontend/src/shared/api/coreApi.js
frontend/src/shared/api/coreIntegration.test.jsx
frontend/src/shared/api/httpClient.js
frontend/src/shared/api/httpClient.test.js
frontend/src/shared/i18n/coreMessages.js
frontend/src/shared/i18n/translate.js
frontend/src/shared/layout/AppShell.jsx
frontend/src/styles/index.css
```
