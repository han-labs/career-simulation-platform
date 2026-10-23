# US-03 / UC-03 — Hoàn thiện UI và hành vi cuộn

Ngày 2026-10-23, Asia/Saigon. HEAD `62bbe1012e9e5c03dad1719b84cd40fff5fce499`
cộng working changes; không commit/stage/push. Giữ nguyên các thay đổi backend,
bootstrap và tài liệu đã có trước nhiệm vụ này.

## Phạm vi và nguồn

Person 1, Student, catalogue hiện hành 10 US / 16 UC: US-03 / UC-03 và năm AC
trong [selected US](../../docs/requirements/user-stories/REPORT_SELECTED_10_US.md),
[UC-03](../../docs/requirements/use-cases/UC-03-complete-career-quiz.md).
Thực hiện yêu cầu trực tiếp “Finalize US-03 UI/UX and Fix Page Scroll Behavior”;
[kế hoạch](../plans/US-03-ui-scroll.md). Phần điều hướng dùng chung được người dùng
cho phép sửa tối thiểu kèm regression. Human reviewer/sign-off còn chờ.

## Nguyên nhân và thay đổi

Giao diện cũ quan sát qua browser hiển thị INTERESTS, profile-sum/1 và danh sách
điểm đơn giản. Code ScrollToTop dùng useEffect + setTimeout(0), chỉ theo pathname/hash,
không theo lần làm hoặc bước nội bộ; chưa quản lý browser scroll restoration.
Không có số đo scrollY trước sửa; bảng dưới là các phép đo sau sửa thực sự chạy.

- Bốn thẻ chọn bài, hướng dẫn, form/progress, history và trạng thái lưu dùng cùng
  token xanh/trắng, typography, spacing, focus và responsive scoped trong assessment.css.
- ProfileAssessmentResult dùng lại nguyên mô tả từ CareerQuizResult thông qua
  profileMetadata: tiêu đề đọc được, tóm tắt, nhóm cao nhất, đồng hạng, thẻ điểm,
  progressbar có min/max/raw và mô tả; Abilities ghi rõ tự tin tự báo cáo.
  Dùng topCategories do backend lưu; không tính lại điểm hay thêm diễn giải nghề nghiệp.
- RIASEC giữ thứ tự sáu nhóm, điểm/range và diễn giải hiện có; điều chỉnh cấp heading,
  styling và vô hiệu hóa nút làm lại khi đang tải. Metadata legacy chỉ được tách file.
- Dịch đầy đủ nội dung mới EN/VI; ngày dùng toLocaleString(locale). Loại mã evaluator
  khỏi UI chính; history dùng tên bài đọc được.
- NavigationScroll dùng useLayoutEffect sau DOM commit, immediate scroll và main
  focus preventScroll; đặt history.scrollRestoration=manual và khôi phục khi unmount.
  Theo location key/path/search/hash; anchor được giữ và cuộn tới mục tiêu có chủ đích.
- useAssessmentScroll theo định danh view/lần làm/trạng thái; không theo answers,
  notice, busy hay locale. Validation focus câu còn thiếu sau commit, bỏ timer 100ms.
- Query `?attempt=` chỉ chọn lần làm để đọc lại qua API owner-authorized hiện có.
  Reload, history và Back/Forward giữ đúng kết quả. Yêu cầu điều hướng mới khi read
  đang chạy được áp dụng sau request hiện tại. Khi đọc history lỗi, retry đúng mục
  được yêu cầu; không trình bày form cũ dưới URL của mục mới; nháp cũ vẫn được giữ.

Chuỗi trách nhiệm: route/page → view/hook → adapter HTTP cũ → backend cũ → stored
result → component trình bày. Không đổi API, DB, evaluator, auth, published version,
Flyway hoặc topology; không có AI/provider call mới. Chỉ tài khoản fixture local.

## Gate thực thi

| Kiểm tra | Kết quả thực tế |
|---|---|
| backend `./mvnw.cmd verify` | PASS: 333 total, 327 PASS, 6 SKIP, 0 failure/error; Spotless/architecture PASS |
| frontend `npm run check`, mặc định | PASS: lint, 133 test / 23 file, build |
| frontend check với VITE_CORE_V2_ENABLED=true | PASS: lint, 133 test / 23 file, build |
| docker compose config --quiet; git diff --check | PASS |
| docker compose up -d --build | PASS; frontend cuối cập nhật bằng up -d --no-deps --build frontend |
| Docker / health | postgres healthy; backend/frontend/gateway Up; /api/health HTTP 200 |
| node scripts/verify-us03-local.mjs --recheck | PASS: exact stored results của bốn loại giữ nguyên sau container restart |

Log local ignored: `.us03-ui-check.log`, `.us03-ui-core-check.log`,
`.us03-ui-backend.log`, `.us03-ui-final-build.log`.
6 SKIP là 5 live opt-in và 1 bootstrap opt-in như gate trước, không tắt test.
jsdom ghi thông báo scrollTo chưa implement ở một số legacy test; scroll mới được
spy kiểm tra và browser thật kiểm chứng. Build Core có cảnh báo chunk 505.60 kB,
không fatal; không thêm dependency hoặc mở rộng task thành tối ưu bundle.

Các test mới bảo vệ mappings/range động 7–35, ties, dữ liệu sai không được diễn giải,
EN/VI, lifecycle/manual restoration, focus/anchors, edit/save không reset, reload,
latest history target khi API pending và retry khi read lỗi. Tests cũ vẫn chạy.

## Browser thực thi tại localhost:8080

CUA IAB, dữ liệu synthetic local, thao tác thật qua Nginx/API/PostgreSQL.
Đã nộp các attempt mới riêng biệt: Interests #16 = 15/15/9/10; Abilities #17 =
15/9/9/9; Work #18 = 15/12/12; RIASEC #19 đủ 42 câu = sáu điểm 28; Interests #20
trên mobile = 9/9/9/10. #21 là nháp phục vụ test, không tạo result. Bản cũ không
bị ghi đè; SQL cuối 4 types / 4 versions / 75 questions / 18 results cho 18 attempts /
22 migrations thành công. #15 nháp có trước được giữ nguyên.

| Bài đã lưu | 1440 px | 768 px | 390 px | 320 px |
|---|---|---|---|---|
| Interests #16 | PASS | PASS | PASS | PASS |
| Abilities #17 | PASS | PASS | PASS | PASS |
| Work Activities #18 | PASS | PASS | PASS | PASS |
| RIASEC #19 | PASS | PASS | PASS | PASS |

16/16: đúng mảng điểm, không mã kỹ thuật, scrollY=0 khi mở; scrollWidth=clientWidth
lần lượt 1425/753/375/305 (15px scrollbar). Kiểm tra lại cả bốn kết quả bằng English;
Vietnamese bao gồm tên/giải thích/limitations/history/ngày giờ. RIASEC và profile
forms thực tế trên mobile không tràn ngang; riêng #19 đánh dấu đủ 42/42 trước nộp.

| Case | Quan sát thực tế | Kết quả |
|---|---|---|
| SCROLL-01 | Chọn Interests từ selection đã cuộn 900px desktop; lặp mobile | PASS: intro y=0 |
| SCROLL-02 | Interests desktop/mobile nộp đủ 12 câu từ cuối form | PASS: saved result y=0, đúng điểm |
| SCROLL-03 | Từ kết quả đã cuộn về selection, desktop/mobile | PASS: y=0 |
| SCROLL-04 | Abilities kết quả đã cuộn → làm lại; mobile đo 740→0 | PASS, lần mới trống, bản cũ giữ riêng |
| SCROLL-05 | Work #14 tablet 1024→0 giữ 9/9/9; #18 mobile 740→0 giữ 15/12/12 | PASS |
| SCROLL-06 | Từ history mở saved results, gồm #12/#17 desktop/mobile | PASS: view mới y=0 |
| SCROLL-07 | Catalogue→dashboard, home và assessment desktop/mobile | PASS; desktop trước 396.8px → dashboard 0 |
| SCROLL-08 | Desktop đổi đáp án cuối form: 2391.2→2391.2; mobile click trực tiếp đáp án 3→4: 3183.2→3183.2 | PASS |
| SCROLL-09 | Back/Forward giữa #18/#1; home/anchor desktop/mobile; deep link /assessment/interests?attempt=16 | PASS; trang thường y=0, anchor giữ đúng mục |
| SCROLL-10 | Lặp desktop/mobile, matrix bốn độ rộng | PASS |

Anchor how-it-works: desktop y=1705.6, target rect.top≈-0.34; mobile y=3188,
target≈-0.16; Back về home y=0, Forward giữ anchor, không vòng cuộn.
Không có text input trong quiz: thao tác edit được kiểm chứng bằng radio; route
test cũng kiểm tra typing không gọi reset. Browser native focus/locator có thể
tự đưa control vào tầm nhìn: hai phép đo locator lệch ~65.6px được ghi là harness
observation, không dùng kết luận app; phép đo click trực tiếp phía trên mới là verdict.

Trong rebuild toàn stack có 502 tạm thời trước khi Spring Boot sẵn sàng; health sau
đó 200, không sửa proxy/security để che lỗi. Phiên JWT hết hạn trong lượt kéo dài:
đăng nhập lại bằng luồng cũ và chạy lại, không kéo dài TTL/bypass auth. Có vài lần
automation đọc trạng thái trước khi tải xong; chỉ kết luận từ trạng thái đã xác nhận.

Bằng chứng local ignored: `tmp/us03-ui-browser-evidence.json`,
`tmp/us03-ui-final-vi.png`, `tmp/us03-ui-interests-1440.png`,
`tmp/us03-ui-abilities-768.png`, `tmp/us03-ui-work-390.png`,
`tmp/us03-ui-riasec-320.png`. Không chứa credential; không stage raw artifacts.

## Files và regression

- `frontend/src/features/assessment/pages/VersionedAssessmentPage.jsx`: chọn bài,
  introduction, history, readable metadata, URL view và retry.
- `components/ProfileAssessmentResult.jsx`, `data/profileMetadata.js`: trình bày
  stored profile; CareerQuizResult import lại metadata nguyên bản.
- `hooks/useAssessmentScroll.js`, `hooks/useVersionedAssessment.js`: view scroll,
  URL read/queued navigation; save/scoring contracts giữ nguyên.
- `assessment.css`, `shared/i18n/assessmentMessages.js`: scoped UI/tokens/dịch mới.
- `AssessmentForm.jsx`, `AssessmentProgressBar.jsx`, `AssessmentResult.jsx`:
  committed validation focus, localized progress, heading/busy state.
- `shared/navigation/NavigationScroll.jsx`, `app/App.jsx`, `shared/layout/AppShell.jsx`:
  thay timer cũ bằng shared layout effect và focusable main. Các US khác không sửa logic.
- Profile/result/page/navigation tests, plan, report và AI log: regression/traceability.

Đã rà git diff; không thay backend/migrations/auth trong increment UI này. Những
thay đổi backend có sẵn trong working tree từ các nhiệm vụ trước vẫn được giữ.
US-01/02 được bảo vệ bằng suite và thực tế sign-in; không đăng ký thêm account trong
lượt UI. Catalogue/dashboard/home/account được smoke browser. Simulation completion
và Agent được bảo vệ bằng suite hiện có; không tuyên bố đã lặp full browser UAT/live
AI/Google. API recheck chứng minh dữ liệu bốn loại trước sửa còn nguyên.

## Còn tồn tại

Không còn lỗi UI/scroll đã xác nhận trong phạm vi kiểm tra. Cảnh báo bundle nêu trên
còn tồn tại. Human review/UAT sign-off, real Google/provider và chất lượng live AI
không thuộc bằng chứng nghiệm thu kỹ thuật local này; vẫn NOT TESTED/PENDING tương ứng.
Không commit, push, deploy hoặc reset/xóa DB.

## Điều chỉnh theo phản hồi: bỏ selector dư và giữ cuộn — 2026-10-23

Yêu cầu mới thay thế hành vi SCROLL-01/chọn hướng dẫn trong phần nghiệm thu trước:
Read instructions là mở nội dung tại chỗ, không còn được coi là chuyển view lớn.
Đã tái hiện trước sửa: đổi dropdown ở URL /assessment làm y≈787.2 về 0 dù URL không
đổi. Nguyên nhân view key chứa introduction và selected. Bản sửa giữ view key là
selection, bỏ hẳn dropdown, giữ card khi mở/đổi panel và không dựng panel mặc định.
Retake quay về card; chỉ Read instructions mới hiện hướng dẫn. Start/Resume, submit,
scoring, local/server draft, version và result contracts không đổi.

Code thực thi chỉ sửa `frontend/src/features/assessment/pages/VersionedAssessmentPage.jsx`.
Tests cập nhật `VersionedAssessmentPage.test.jsx` và đúng các case US-03 trong
`frontend/src/shared/api/coreIntegration.test.jsx`; không đổi test/logic US khác.
Kế hoạch và nhật ký AI hiện có được bổ sung theo workflow. Không sửa hook persistence,
shared navigation, CSS, API, backend, DB/migrations, scoring hoặc môi trường. Đã
so hash các working changes có trước và rà diff để giữ nguyên phần ngoài phạm vi.

Gate mới thực thi:

- Focused hai test file: 35 PASS (chạy --maxWorkers=1 khi backend gate đang dùng tài nguyên).
- `npm run check` mặc định và VITE_CORE_V2_ENABLED=true: mỗi cấu hình **136 PASS /
  23 files**, lint/build PASS. Cảnh báo Core bundle 505.38 kB không fatal.
- Backend không sửa đổi: `./mvnw.cmd verify` **333 total, 327 PASS, 6 opt-in SKIP,
  0 failure/error**, BUILD SUCCESS. Tests dùng DB/container riêng.
- Compose config, /api/health HTTP 200, git diff --check: PASS. Chỉ rebuild frontend
  bằng `docker compose up -d --no-deps --build frontend`, không restart backend.

Lượt full test đầu có bốn case US-03 còn kỳ vọng dropdown cũ, một selector test khớp
whitespace quá chặt và một timeout trong lúc gate backend chạy song song. Đã cập
nhật đúng kỳ vọng UI và tăng bảo vệ đúng version khi Start; không xóa test, tăng
timeout hay tắt gate. Lượt full bình thường sau khi backend xong PASS cả hai cấu hình.

Browser CUA thực ở localhost:8080:

- Desktop 1440px: mặc định 4 card, 0 dropdown trong main, 0 panel; đổi lần lượt
  Interests / Abilities / Work Activities / RIASEC đều giữ y=224.8 và URL cũ,
  đúng một panel với tên bài tương ứng. Không reload, không gọi start khi chọn card.
- Mobile 320px: mặc định không có dropdown/panel, scrollWidth=clientWidth=305;
  mở Interests và đổi sang Abilities bằng click trực tiếp giữ y=740 và URL.
  Một click tọa độ ban đầu trượt nút Abilities được đánh dấu không đạt trong raw
  harness log; đã đo lại bằng tâm nút lấy từ DOM và xác nhận đúng heading mới.
- Mở nháp có sẵn #21: 12 câu Abilities, y=0. Mở kết quả #17: y=0, giữ đúng điểm
  15/9/9/9. Refresh selection đã cuộn: y=0, panel=0, dropdown=0.
- Không tạo/nộp quiz mới trong lượt browser này. Submit, retry, ownership, history,
  retake và các scoring UI regression vẫn được bảo vệ trong suite PASS.

Bằng chứng local ignored: `.us03-selector-check.log`, `.us03-selector-core-check.log`,
`.us03-selector-backend-verify.log`, `.us03-selector-focused-final.log`,
`tmp/us03-selector-browser-evidence.json`, `tmp/us03-selector-final.png`,
`tmp/us03-selector-mobile.png`. HEAD vẫn 62bbe1012e9e5c03dad1719b84cd40fff5fce499
+ working changes; không commit/push. Human sign-off vẫn chờ, không bổ sung tính năng.
