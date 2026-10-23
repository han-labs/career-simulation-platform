# US-03 / UC-03 — ổn định runtime và kiểm chứng

Ngày 2026-10-23. HEAD `62bbe1012e9e5c03dad1719b84cd40fff5fce499` + các thay đổi
chưa commit có sẵn. Phạm vi: Person 1, current selected US-03/UC-03. Không triển khai
US-01/US-02; không thay auth, scoring, migration, permissions hoặc publish dữ liệu local.

## Root cause và thay đổi

Startup từng lỗi thiếu JWT key đã được xử lý trong nhiệm vụ runtime trước. Lần này
kiểm tra lại stack đang running, PostgreSQL healthy, gateway health 200, log không còn
startup failure, Flyway validated 22 migrations/schema up to date.

Phát hiện qua component regression: metadata `assessment-start` có requestId
`damaged-cache` được gửi nguyên lên API thay vì UUID. Backend contract nhận UUID nên
đây là request không hợp lệ, gây lặp lỗi bắt đầu quiz khi dùng lại cache hỏng.
Test trước sửa: 16 PASS / 1 FAIL, received `damaged-cache` khi cần UUID hợp lệ.
Sửa tối thiểu `useVersionedAssessment.js`: chỉ tái sử dụng chuỗi UUID đúng định dạng,
thay metadata hỏng bằng UUID mới. UUID hợp lệ vẫn giữ nguyên cho retry/idempotency.
Không đổi đáp án, score hoặc nội dung attempt đã lưu.

## Ma trận kiểm chứng

| AC/flow | Kiểm chứng |
|---|---|
| AC1 chọn độc lập | component typed route/catalogue; PG independent methods + optional simulation access |
| AC2 hướng dẫn/questions | component RIASEC/Interests/Abilities/Work Activities; publication/schema validation |
| AC3 field guidance | missing/invalid local values, EN/VI, error association, focus; backend malformed/missing/duplicate/wrong-version rejection |
| AC4 submit/result | deterministic evaluator; persisted result, min/max display; save failure/retry |
| AC5 history | PG fixture mới: 4 loại × 2 version × 2 lần làm = 16 attempt/result riêng; owner khác không đọc được |
| Main | start → save draft → read lại qua transaction → submit → retrieve result trên PG thật cô lập |
| Alternative postpone/retake | component postpone/resume, UUID retry, retake; PG multiple attempts |
| Exceptions 2.1–2.3 | component missing/unavailable/save errors; PG invalid input, archived type, rollback test hiện hữu |
| Concurrency / privacy | PG parallel submit, idempotency, immutable content, owner isolation; frontend explicit draft conflict resolution |

Fixture mới trong CorePackageAIntegrationTest dùng questionnaire tổng hợp. Các tên
loại có suffix FIXTURE/UUID; không phải reviewed questionnaire và không chứng nhận
validity của RIASEC/profile content. Testcontainer có DB riêng; không publish vào
Compose DB hiện hữu, không cần Google hoặc bổ sung tính năng đăng ký/đăng nhập.

## Giới hạn và completion

GET `/api/v2/assessment-types` local trả 200 và danh sách rỗng. Vì vậy hoàn tất quiz
trên browser local bằng nội dung được phê duyệt vẫn **BLOCKED**. Không tạo kết quả giả,
không chuyển bốn legacy quiz thành reviewed v2, không tắt authorization để demo.
Responsive form/result đầy đủ và persistence sau restart có dữ liệu trên stack local
vẫn **NOT TESTED**. Read-back PG ở đây xác nhận dữ liệu đã commit qua transaction khác,
không được diễn giải thành bài test restart PostgreSQL.

Phương án test an toàn hiện dùng: fixtures + PostgreSQL Testcontainers hiện hữu.
Sau khi Admin cung cấp/publish questionnaire đã duyệt, dùng identity phát triển đã
được phép để UAT UI; không cần Google. Human content/UAT sign-off vẫn pending.

## Files riêng lần này

- `frontend/src/features/assessment/hooks/useVersionedAssessment.js`: phục hồi start metadata hỏng.
- `frontend/src/features/assessment/pages/VersionedAssessmentPage.test.jsx`: regression trước/sau sửa.
- `backend/src/test/java/edu/hcmute/careersim/system/migration/CorePackageAIntegrationTest.java`: 4 loại, drafts, retakes, versions, ownership trên PG cô lập.
- `.agent/plans/US-03-career-quiz-alignment.md`: bổ sung kế hoạch stabilization trước sửa.
- Báo cáo này và AI log tương ứng. Toàn bộ thay đổi các nhiệm vụ trước được giữ lại.

## Kết quả cuối

| Lệnh/check thực tế | Kết quả |
|---|---|
| `npm run test:run -- src/features/assessment/pages/VersionedAssessmentPage.test.jsx` trước sửa | FAIL có chủ đích: 16 PASS, 1 FAIL (malformed UUID) |
| `backend/.\mvnw.cmd spotless:apply verify` sau sửa | PASS: 331 tổng, 326 PASS, 0 FAIL/ERROR, 5 live SKIP; BUILD SUCCESS |
| CorePackageAIntegrationTest trong full verify | PASS 28 test, gồm fixture bốn quiz/16 attempt-result mới |
| `frontend/npm run check` | PASS: 116 PASS, 0 FAIL/SKIP, lint/build PASS |
| `$env:VITE_CORE_V2_ENABLED='true'; npm run check` | PASS: 116 PASS, 0 FAIL/SKIP, lint/build PASS |
| `docker compose up -d --build` | PASS, rebuild xong; postgres healthy, backend/frontend/gateway running |
| Gateway `/api/health` | PASS HTTP 200 sau rebuild |
| Gateway `/api/v2/assessment-types` | HTTP 200, 0 quiz: BLOCKED browser completion |
| `docker compose config --quiet`, `git diff --check` | PASS |
| Git diff migrations/auth backend/frontend so với HEAD | Rỗng; không sửa các phạm vi đó |

Regression full suite xác nhận các test hiện hữu cho simulation completion/self-review,
legacy flags, owner/role security, Agent/upgrade và kiến trúc vẫn PASS. Đây là kiểm
chứng kỹ thuật trong môi trường fixture, không phải UAT hoặc Google/live AI acceptance.
Các cảnh báo connection pool đến container test đã teardown không làm test thất bại;
backend Compose hiện hữu vẫn healthy và không có fatal exception mới.

Không commit/push/deploy/reset DB. Chưa đánh dấu toàn bộ UC-03 accepted. Bước còn lại:
Admin cung cấp nội dung bốn loại đã duyệt (hoặc xác định loại nào hỗ trợ), publish
đúng version; sau đó UAT desktop/mobile, submit/refresh/restart/retake trên nội dung đó.
