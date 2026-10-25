# US-03 / UC-03 — Đối chiếu và triển khai Career Quiz

Ngày 2026-10-23 (Asia/Saigon). Catalogue hiện hành: 10 US / 16 UC, Person 1.
Baseline commit: `62bbe1012e9e5c03dad1719b84cd40fff5fce499`.
Nhánh: `codex/us-03-career-quiz-alignment`; mã thay đổi chưa commit.
Nguồn: [kế hoạch](../plans/US-03-career-quiz-alignment.md),
[UC-03](../../docs/requirements/use-cases/UC-03-complete-career-quiz.md),
[AC](../../docs/requirements/user-stories/REPORT_SELECTED_10_US.md#us-03--complete-career-quiz),
Baseline v1 US-04/DR1–DR2, P1/P2 và mã/test hiện hành.

## Phạm vi và luồng

| AC/flow | Mã và thay đổi | Bằng chứng / giới hạn |
|---|---|---|
| AC1 chọn bài độc lập | Chỉ dùng catalogue published; giữ deep link theo đúng type; liên kết Simulation độc lập | coreIntegration + VersionedAssessmentPage tests; không seed nội dung |
| AC2 hướng dẫn/câu hỏi | Hướng dẫn RIASEC/Interests/Abilities/Work Activities; loại cấu hình khác dùng thang số 1–5; câu hỏi ghim phiên bản | Component tests ba loại profile và RIASEC; backend publication tests |
| AC3 hướng dẫn tại trường | Thông báo đúng UC, aria-invalid/describedby, focus câu thiếu đầu tiên; không coi chuỗi/số sai là đáp án hợp lệ | Component EN/VI, malformed local data; evaluator schema tests |
| AC4 nộp/xem summary | Giữ backend deterministic; UI dùng raw/min/max từ result lưu thật; lỗi không tạo thông báo hoàn thành | Component save failure/retry + evaluator/service/controller tests |
| AC5 kết quả riêng | API lịch sử chỉ owner, đọc attempt/result đúng type/version; retake tạo request mới | History component/controller/service tests; PG test bổ sung, cần Docker |
| Main 1–2 | Chọn → start idempotent → hướng dẫn → trả lời → validate → lưu/score → summary | Unit/component PASS; tích hợp PG/gateway chưa đủ điều kiện |
| Alternative 1a | Tạm hoãn về selection, giữ draft, không gọi submit; link Simulation luôn còn | Component postpone/resume test |
| Alternative 2a | Lịch sử 100 attempt gần nhất gồm draft/submitted, giữ riêng type/version | Component history test; persistence dùng bảng hiện hữu |
| Exception 2.1 | Thiếu bắt buộc/giá trị sai: giữ form, đánh dấu câu hỏi, không submit | Component và evaluator tests |
| Exception 2.2 | Version/type không khả dụng: chặn start/save/submit mới; về selection; đọc lịch sử vẫn được | Service tests; PG archived-type test đã thêm |
| Exception 2.3 | Lỗi lưu/network: giữ câu trả lời, thông báo không lưu được, retry cùng payload | Component failure/retry; rollback test PG hiện hữu chưa chạy được |
| Draft/concurrency/idempotency | Owner+attempt+revision; chọn rõ server/local khi conflict; submitted bỏ qua local; in-flight guard; giữ start UUID khi mất response | Component tests; PG parallel-finalization/rollback hiện hữu cần chạy lại |

## Trách nhiệm và dữ liệu

`VersionedAssessmentController → VersionedAssessmentService → VersionedAssessmentServiceImpl
→ VersionedAssessmentDao / evaluator → PostgreSQL`.
`VersionedAssessmentPage → useVersionedAssessment / components → assessmentApi → shared HTTP`.
Không đổi topology, scoring/evaluator semantics, quyền, authentication, simulation,
dashboard/Agent/enterprise, cờ mặc định hay schema/migration. Chỉ sửa nội dung thông báo
required trong evaluator. Dùng lock type khi kiểm tra khả dụng cho write; bản published
cũ vẫn hợp lệ khi có bản mới. Retry submit đã hoàn thành trả result cũ, kể cả type đã archive.
Không backfill hoặc rewrite lịch sử. `git diff` so với HEAD cho migrations/Baseline rỗng.

API additive `GET /api/v2/assessment-attempts` trả metadata 100 attempt của actor,
không trả đáp án và không nhận owner để phân quyền. Không phụ thuộc guidance dashboard.
Chi tiết: [assessment API](../../docs/api/assessment-uc03.md).

Không có product AI mới hoặc lời khẳng định phù hợp nghề nghiệp. No composite score.
Core demo links không áp profile cũ lên câu hỏi mới; legacy demo vẫn qua form validation.
Không tự chuyển bốn quiz cũ thành bản v2 đã rà soát/publish.

## Kiểm tra đã thực thi

Kết quả cuối cùng được cập nhật sau khi lệnh hoàn tất; không dùng số liệu P1–P6 cũ
làm kết quả của lần này. Các log `.us03-*.log` là file tạm bị ignore, không phải tài
liệu đã được duyệt để công bố.

| Lệnh/check | Kết quả quan sát |
|---|---|
| Baseline `backend/.\mvnw.cmd verify` trước sửa | 240 theo Surefire: 212 PASS, 0 assertion FAIL, 8 ERROR do thiếu Docker, 20 SKIP; gate FAIL/BLOCKED. Lần đầu bị cache .m2 read-only; retry có quyền cache chạy được |
| Baseline `frontend/npm run check` trước sửa | BLOCKED: thiếu eslint/dependencies; `npm ci --no-audit --no-fund` sau đó thành công, lockfile không đổi |
| `backend/.\mvnw.cmd spotless:apply test -Dtest=VersionedAssessmentEvaluatorTest,VersionedAssessmentServiceTest,VersionedAssessmentControllerTest` | PASS: 19/19, 0 FAIL/ERROR/SKIP |
| `frontend/npm run check` mặc định | PASS bản cuối: 115/115, 0 FAIL/SKIP, lint/build PASS (16:04) |
| `$env:VITE_CORE_V2_ENABLED='true'; npm run check` | PASS bản cuối: 115/115, 0 FAIL/SKIP, lint/build PASS (16:05); chỉ biến của tiến trình |
| `docker compose config --quiet` | PASS, exit 0 |
| `backend/.\mvnw.cmd verify` bản cuối | FAIL/BLOCKED: 259 theo Surefire, gồm 231 PASS / 0 assertion FAIL / 8 ERROR / 20 SKIP (16:02). Cả 8 ERROR là môi trường Docker, không chạy được các lớp PG |
| `backend/.\mvnw.cmd spotless:check` | PASS, exit 0 (16:04); verify dừng trước phase format nên kiểm tra riêng |
| `git diff --check` | PASS; migrations V1–V22 và frozen Baseline không có diff so với HEAD |
| Docker API | Ban đầu không có pipe; đã thử `docker desktop start` và khởi động Desktop ẩn. Probe `docker info` có timeout 15s vẫn không phản hồi. Không chạy/reset DB hiện hữu |

Một lượt Vitest trong lúc hai Maven process cùng chạy có 108 PASS / 2 timeout ở test
Admin hiện hữu. Chạy riêng lại không đổi test: 115 PASS. Một lượt chạy trong sandbox
bị EPERM rename cache trước khi thực thi test; retry được quyền chạy thì hoạt động.
Không nới timeout, bỏ assertion hay disable test.

## Browser / hồi quy / giới hạn

Vite Core tại loopback port 5175 mở được sau một lần tab attach timeout. Đã quan sát
Landing, Dashboard, Simulation và Assessment qua browser để đối chiếu shell, headings,
spacing. Backend chưa chạy nên Dashboard/Simulation/Assessment nhận 502 qua Vite proxy;
đây không phải chứng cứ chức năng thành công. Phát hiện và sửa lỗi hiển thị catalogue
rỗng khi tải thất bại; test thêm assertion tránh thông báo sai.

Viewport 320×720: clientWidth=scrollWidth=305 (15px scrollbar), không cuộn ngang ở
Assessment loading/error. Quan sát EN/VI và nút retry; đã reset viewport sau kiểm tra.
Không có browser success-flow/result/history với PG thực. Responsive form/result mới
có CSS/component evidence, chưa phải UAT đầy đủ.

Frontend suite giữ các kiểm tra HTTP/CSRF/account generation, legacy drafts/demo,
simulation final confirmation/zero-score/self-review, Syn và Admin/Student Agent.
Backend unit/controller/architecture chạy được; các lớp PG auth/Core/Agent/upgrade,
concurrency, rollback cần Docker khả dụng. Không gọi Google/provider thật.

## Files và bàn giao

- Backend assessment: controller, DTO, service interface/implementation, DAO,
  evaluator message; 3 test class mới; thêm 1 test vào CorePackageAIntegrationTest.
- Frontend assessment: page, hook, API adapter, scoped CSS, instructions;
  Form/QuestionCard/Result, result tests và page tests.
- i18n: dictionary additive assessmentMessages + một import/merge; kiểm tra full suite.
- Tài liệu: plan, API contract, UC-03 verification links, alignment follow-up,
  báo cáo này và AI-assistance log. Không sửa specification US/UC khác.

Chưa commit/push/merge/deploy. Person 1 + reviewer cần rà code/UAT và content.
Không thể đánh dấu toàn UC accepted trước khi có PostgreSQL/gateway success flows,
content publication được người có thẩm quyền duyệt, và human sign-off.

## Kết quả xác minh cuối

19 test backend mới PASS; 231 test backend thực thi PASS, 8 lỗi khởi tạo lớp Docker,
20 SKIP. Tám lớp bị chặn: AuthenticationContractTest, CorePackageAIntegrationTest,
AgentPlatformIntegrationTest, StudentAgentIntegrationTest, SchemaUpgradeIntegrationTest,
SecurityCoreUpgradeIntegrationTest, AgentPlatformUpgradeIntegrationTest,
StudentAgentUpgradeIntegrationTest. Surefire không đếm đầy đủ các method trong lớp
không khởi tạo được; không dùng 259 làm số test acceptance đầy đủ.

Docker/gateway/health và luồng nộp bài với PostgreSQL: **BLOCKED / NOT TESTED**.
Browser responsive form/result đầy đủ, nội dung published thực và human UAT: **NOT TESTED**.
Không đánh dấu UC-03 accepted. Không thay đổi production env, không reset DB.

Frontend mặc định và Core cuối cùng đều 115 PASS / 0 FAIL / 0 SKIP, lint/build PASS.
Compose config, Spotless và Git diff checks PASS. Đã dừng Vite preview và các Docker
CLI probe bị treo do phiên làm việc tạo; không stop/reset database hay container hiện hữu.
