# US-03 / UC-03 — bằng chứng local demo

2026-10-23, Asia/Saigon. HEAD 62bbe1012e9e5c03dad1719b84cd40fff5fce499 + working
changes; không commit. Giữ nguyên mọi thay đổi từ các nhiệm vụ trước. Catalogue hiện
hành US-03/UC-03, Person 1; user đã yêu cầu rõ bootstrap bốn assessment local.

## Nguyên nhân và giải pháp

V4/V10/V11 seed câu hỏi legacy, V16 tạo bảng Core nhưng không tự publish questionnaire
v2. Đã xác nhận API 0 type và SQL 0 version/question trước bootstrap. Không phải lỗi
DB connection hay scoring; trước đó stack đã được sửa JWT config và chạy healthy.

Thêm opt-in bootstrap trong test source, bảo vệ profile/flag/JDBC loopback; dùng
inventory legacy + dịch EN/VI và publication service hiện có. Không sửa main auth,
không thêm impersonation endpoint, không tắt CSRF/authorization. Synthetic publisher
không có password; Student fixture dùng password ngẫu nhiên file ignored. Local
Compose override giới hạn bốn port về 127.0.0.1, không đổi base deployment config.

Bootstrap transaction rollback toàn bộ khi thiếu translation ở lần đầu: SQL vẫn 0
type/version. Bổ sung 33 bản dịch profile rồi chạy thành công. Rerun nhiều lần vẫn
4 types / 4 versions / 75 questions; không update published content hoặc result.
IDs version không liên tục do sequence cấp trước rollback: RIASEC 2, Interests 3,
Abilities 4, Work Activities 5. Không renumber sequence/history.

Frontend sửa category code thành label đọc được, dịch EN/VI, giữ raw/min/max từ
server. Không sửa methodology, không tính lại điểm client và không tạo composite.

## Kết quả AC / flow thực thi

| AC/flow | Evidence thực tế | Kết quả |
|---|---|---|
| AC1 lựa chọn | Browser dropdown đủ 4 type, lần lượt chọn bài qua published API không prerequisite | PASS local |
| AC2 instructions/questions | Browser từng loại 42/12/12/9 câu, type-specific response labels; bootstrap đủ EN/VI/category coverage | PASS local |
| AC3 required | Click Submit trống ở cả bốn bài: required alert, không result; API thiếu/giá trị 0 trả 422 và result 404 | PASS local |
| AC4 submit/summary | Browser chọn 5: RIASEC sáu chiều 35/35 (7–35), profile các category 15/15 (3–15); HTTP actual persisted result | PASS local |
| AC5 history | Browser F5 rồi View saved result ở từng loại; 4 kết quả đầu riêng (#1–4); retake Work #5 giữ cũ, profile mức 3 tạo #14 với 9/15; API retake riêng. Multi-version: PG integration thật 4 types×2 versions×2 attempts, không mock DB | PASS kỹ thuật, phân biệt browser và fixture |
| Postpone/resume | Browser Save draft RIASEC, F5, Resume giữ câu; Work draft #14 đã commit, sau PG restart vẫn 9 câu; component postpone không submit | PASS |
| Conflict | API ghi revision mới vào Work draft #5 trong lúc browser còn revision cũ; Save draft UI chặn, yêu cầu chọn rõ Keep my local answers; giữ 9 câu | PASS thực |
| Network failure | Stop backend local, Submit → lỗi không lưu được, vẫn 9 câu, không completed giả; start lại và retry thành công. CSRF phiên server cũ cần retry/refresh theo transport hiện có | PASS thực |
| Persistence failure | Stop PG local, submit draft #14 → lỗi lưu, vẫn form/9 câu. Khôi phục PG + backend, F5/Resume rồi submit cùng attempt → 9/15, không mất draft | PASS thực |
| Restart/history | API snapshots bốn loại được đối chiếu chính xác sau restart PG/backend và sau đổi local port bindings | PASS thực |
| Ownership/idempotency | HTTP hai Student thực: foreign result/write 404, history không lộ owner khác, retry same submit trả result cũ, changed payload409; stale draft409 | PASS thực |
| Unavailable/immutability | HTTP unknown version404; component mapping unavailable và PG archived-type/published immutability tests thật trong gate | PASS theo từng lớp, không tuyên bố UI retired-version end-to-end |
| Mobile/a11y | Browser 320×740 form và result: clientWidth=scrollWidth=305, không cuộn ngang; form VI 9 câu, giữ 9 selected; component focus/error association | PASS; không phải audit WCAG toàn diện |

Screenshots local ignored: tmp/us03-mobile-form.png, tmp/us03-mobile-result.png,
tmp/us03-desktop-result.png, tmp/us03-desktop-riasec.png. RIASEC gốc #1 cũng đã được
mở lại qua browser sau restart, vẫn sáu chiều 35/35. Tất cả dùng dữ liệu tổng hợp,
không hiển thị password.

## Commands và quality gates

- `scripts/setup-us03-local.ps1`: opt-in bootstrap PASS; reruns PASS, vẫn 4/4/75.
- `scripts/verify-us03-local.mjs`: PASS bốn loại, retake, negative/ownership; lần đầu
  bị gateway 429 khi gửi quá nhanh. Sửa nhịp script 600ms, giữ nguyên rate limit và
  assertions. Không coi lần 429 là lỗi validation backend.
- `node scripts/verify-us03-local.mjs --recheck`: PASS exact stored results sau restart.
- `backend/.\mvnw.cmd spotless:apply verify`: BUILD SUCCESS, 333 tổng, **327 PASS /
  0 FAIL/ERROR / 6 SKIP** (5 live + 1 opt-in bootstrap). Guard test chạy; bootstrap
  không tự chạy vào DB local trong full gate.
- Frontend `npm run check` mặc định và VITE_CORE_V2_ENABLED=true: mỗi chế độ **117
  PASS / 0 FAIL/SKIP**, lint/build PASS.
- Compose build/start, config validation, health200, SQL read-only inventories,
  diff checks; migrations và main identity files không có diff so HEAD.

Full regression chứa simulation completion/self-review/independent access, Core/Auth,
Agent/upgrade, legacy flags và architecture. Không triển khai US-01/US-02; chỉ dùng
sign-in existing để vào Student fixture. Không gọi Google hoặc live provider.

## Phạm vi và giới hạn bàn giao

Inventory cuối: SQL 4 types / 4 versions / 75 questions, 13 results ứng với 13 distinct
attempt IDs (các lượt test/retake có chủ đích). Bản draft gián đoạn do rate limit vẫn
được giữ để resume, không xóa nhằm làm sạch evidence. Check JAR production tìm
LocalCareerQuizBootstrap/LocalQuizBootstrapGuard: 0 class. Credentials và .env được
git check-ignore xác nhận. Bốn service đang running trên loopback; postgres healthy.

Files riêng increment này: scripts/setup-us03-local.ps1, scripts/us03-translations.mjs,
scripts/verify-us03-local.mjs; ba class test-source LocalCareerQuizBootstrapTest,
LocalQuizBootstrapGuard, LocalQuizBootstrapGuardTest; docker-compose.us03-local.yml;
assessmentInstructions/categoryLabel, VersionedAssessmentPage/category rendering,
assessmentMessages/profile translations và page test; setup/UAT, plan, alignment,
UC-03 tracking, báo cáo và AI log. Không sửa base Compose, main identity hay migrations.

Local demo AC1–AC5 đã có evidence thực, bốn quiz hoạt động và kết quả sống qua restart.
Đây là technical acceptance readiness; không phải psychometric certification, human
sign-off của khóa luận hay production rollout. Bản dịch profile mới cần review ngôn
ngữ/học thuật khi đưa ra ngoài local demo; không khẳng định đã có human review.
Production/shared deployment, live Google/AI, NFR và audit WCAG đầy đủ NOT TESTED.
Không còn blocker kỹ thuật đã biết cho local US-03. Không commit/push/reset/deploy.

Setup và các bước UAT: [hướng dẫn local](../../docs/testing/us03-local-demo.vi.md).
