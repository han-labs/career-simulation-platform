# US-03 / UC-03 — UI và điều hướng cuộn

Status: Implemented, local technical verification PASS; human sign-off pending. Date: 2026-10-23.
Actor: Student. Owner: Person 1; human reviewer/sign-off pending.
Shared navigation owner: current task, explicitly coordinated by the user's instruction to fix route scrolling minimally.

## 1 Scope and sources

Current report 10 US / 16 UC, selected US-03 / UC-03 and its five AC;
`docs/requirements/use-cases/UC-03-complete-career-quiz.md`, `.agent/DESIGN.md`,
latest user task “Finalize US-03 UI/UX and Fix Page Scroll Behavior”. Existing
blue/white tokens take precedence over historical warm-palette guidance.
Preserve all uncommitted work and Person 2/3 features.

## 2 Flows and acceptance

Choose any of four published quizzes → instructions → required questions → stored
result → history/retake. Missing answers remain field validated; network/conflict
failures preserve answers; unavailable versions return selection. No AI involved.
Polish profile summaries using existing CareerQuizResult metadata, server scores,
server topCategories (including ties) and min/max. Localize EN/VI, dates and labels.
Scroll after committed navigation only: routes, selected quiz, form/result/history
entry/selection/retake and reload. Preserve anchors and answer/edit positions.
Keep current result in a URL query for read-only reload/history navigation; do not
change persistence or API contracts.

## 3 Responsibilities and surface

Page composes selection/instructions/forms/history; ProfileAssessmentResult projects
stored scores with shared approved metadata; existing hook/API remain authoritative
for saves. Shared navigation layout effect manages manual browser restoration and
route anchors. Assessment layout effect handles committed view identity/focus.
Frontend only; no backend, evaluator, authentication, schema, migration or env change.
No new dependencies. Evidence in `.agent/reports/us-03-ui-scroll.vi.md` and AI log.

## 4 Security, privacy and API

Existing authenticated owner checks and draft keys unchanged. Query attempt ID is
an existing opaque identifier, never authorization; API enforces ownership. No new
data sent to AI or third parties. Synthetic local fixture only for browser testing.

## 5 Verification

Focused profile tests: all category mappings, server ranges, invalid data, ties,
EN/VI. Navigation tests: route/search/Back/Forward/anchors/lifecycle; view tests:
submit/history/retake transitions and no reset on answers/save/locale.
Run backend verify, frontend check default/Core, Compose config and local rebuild.
Browser SCROLL-01–10 and all four results; widths 1440/768/390/320; reload/history
and retake preservation. Record observed PASS/FAIL/NOT TESTED, exact HEAD and limits.

## 6 Boundaries

No scoring, published content/version, auth, persistence or other US implementation
changes. Human content review and final sign-off remain separate from local tests.
No commits, push, deployment or database reset.

Executed evidence: [UI/scroll report](../reports/us-03-ui-scroll.vi.md), backend 327
PASS/6 opt-in SKIP, frontend default/Core each 133 PASS, browser matrix16/16 and
SCROLL-01–10. Existing results preserved; bundle warning/human review remain recorded.

## Điều chỉnh selector theo yêu cầu tiếp theo — 2026-10-23

Phạm vi được duyệt: chỉ frontend US-03, Student / Person 1, cùng UC-03 và năm AC.
Bỏ dropdown và panel hướng dẫn mặc định; bốn card là nơi chọn bài duy nhất.
Read instructions mở/cập nhật panel tại chỗ, không thay URL, reload, focus hoặc cuộn.
Giữ card trong DOM để mở panel không làm co trang gây dịch chuyển không cần thiết.
Chỉ view lớn (selection / quiz / result / loading) tham gia scroll reset; selected
type và trạng thái mở panel không phải navigation. Start/Resume, submit, stored
result, retry/error/history/retake contracts không đổi. Retake quay về card; chỉ
hiện hướng dẫn sau Read instructions theo yêu cầu mới.

Chỉ sửa VersionedAssessmentPage.jsx và test liên quan (kể cả các case US-03 trong
coreIntegration.test.jsx); không sửa shared navigation,
backend, hook persistence, API, scoring, DB, môi trường hoặc US khác. Ghi bổ sung
bằng chứng vào report/AI log US-03 hiện có theo workflow. Kiểm tra default không có
dropdown/panel, chọn cả bốn bài không gọi API/scroll/navigation; quiz/results vẫn
reset, draft/retake không mất. Chạy frontend check default/Core và browser desktop/
mobile local; giữ nguyên working changes có trước, không commit/push.

Hoàn thành follow-up: focused 35 PASS; frontend check default/Core mỗi 136 PASS,
lint/build PASS; backend 327 PASS/6 opt-in SKIP; browser desktop 1440/mobile 320
xác nhận chọn hướng dẫn không đổi cuộn, quiz/result/reload vẫn về đầu. Bằng chứng
được ghi bổ sung trong report và AI log hiện có; human review còn chờ.
