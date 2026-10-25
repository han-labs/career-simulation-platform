# AI assistance — US-03 local demo

Ngày 2026-10-23, Codex. User yêu cầu gỡ blocker nội dung, publish bốn quiz trong local
và kiểm chứng UI/API/persistence. Đọc current UC/AC/plan, source authoring/evaluator,
legacy questions/EN/VI, auth fixture boundaries và rules đã đọc trong phiên này.

Hỗ trợ: bootstrap test-source có guard/local profile, CLI setup/HTTP verifier,
category labels, 33 bản dịch VI, regression test và chạy browser thật. Không thêm
auth endpoint, không giảm security, không dùng subagent hay product LLM calls.
Không đưa password/token vào report/Git. Không sửa migrations hoặc result cũ.

Human review: nội dung nguồn tái sử dụng theo yêu cầu cho local demo; bản dịch mới
và học thuật cần con người duyệt trước khi public/production. Không gán psychometric
validity cho questionnaire. Từ chối tự update published content khi fingerprint khác.

Evidence gồm cả rollback do thiếu translation, API429/rate-limit pacing, real stop/
restart failure/retry và technical acceptance: [báo cáo](../us-03-local-acceptance-2026-10-23.vi.md).
Không commit/push/deploy/reset DB. Full gate skip opt-in bootstrap nhưng guard chạy;
bootstrap được chạy riêng có chủ đích và chứng minh idempotency trên DB local.
