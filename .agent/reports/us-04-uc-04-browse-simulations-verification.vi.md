# US-04 / UC-04 Browse Simulations - Báo cáo triển khai và kiểm chứng

## Kết luận closure — 2026-10-25

**PO báo cáo UAT: 14 PASS / 1 PARTIAL / 1 BLOCKED / 0 FAIL.** Codex nhận kết quả từ PO, không trực tiếp quan sát toàn bộ phiên UAT. TC-04-04A Career Track dropdown PASS đủ 8 checks; TC-04-05 PARTIAL vì 9 scenarios nằm trên một trang; TC-04-12 Google OAuth BLOCKED vì local tắt Google. Không tự suy diễn kết quả riêng của các case chưa được PO cung cấp. Survey trước enrollment là mục review yêu cầu UC-05, không phải lỗi UC-04 tự động (alignment D10).

Nguồn hiện hành: [evidence/UAT](../../docs/evidence/US-04/us-04-browse-simulations.md), selected US-04/UC-04, quyết định D09 (D03 đã trỏ cùng quyết định). Career Track là dropdown lấy giá trị public API qua đủ trang; Industry IT cố định. PO hiện phụ trách UC-04/05/06; UC-09 thuộc đồng đội, không nằm trong closure này. Các checkpoint bên dưới được giữ như lịch sử, không đại diện trạng thái cuối.

### A. Inventory thực tế

- HEAD `9ee96e6ae9da3d63d100cebd99ee862dccbc938d`, nhánh `feature/browse-simulations`, base `62bbe1012e9e5c03dad1719b84cd40fff5fce499`. CP1 đã commit 15 files: industry classification/filter/cursor, V23 và tests. Không stage lại CP1.
- Chưa commit: 20 tracked modified + 8 text/source untracked = **28 text/source paths**; thêm **11 PNG UAT** do PO cung cấp, giữ ngoài whitelist công khai. Evidence hiện ở `docs/evidence/US-04/`, không tạo lại vị trí cũ.
- Feature: public metadata/media/task outline/skill names; card link, public detail/anchors/disclosure, search/filter/sort/cursor/URL, Career Track dropdown; alias routes; login-return nội bộ; EN/VI/CSS; CDN/Wistia CSP.
- Migration: V24 schema public media kèm mapping; V25/V26 sửa mapping slug; V27 sửa workplace context. V23 thuộc CP1. Không sửa SQL trong closure.
- Tests: controller public DTO, industry/mapping integration, public-detail negative fields trong Core integration, browse/login-return frontend.
- Docs: plan, report, relocated evidence, evidence index, traceability, UC-04, alignment, AI_USAGE.

### B–C. Verification thực thi trong closure

| Lệnh/phạm vi | Kết quả thực tế |
|---|---|
| `vitest run src/features/simulations/pages/coreBrowse.test.jsx` | **19 PASS, 0 FAIL, 0 SKIP** |
| Scoped ESLint: App, AccountPage, SimulationCatalog, CoreCatalogPage, CoreSimulationPage, coreBrowse test, coreMessages | **PASS** |
| `VITE_CORE_V2_ENABLED=true`, `VITE_AGENT_PLATFORM_ENABLED=false`, `VITE_API_BASE_URL=/api`; `npm run build` | **PASS**, 1931 modules |
| Maven JDK17: VersionedCatalogControllerTest | **3 PASS** |
| ScenarioClassificationServiceTest | **5 PASS** |
| ScenarioAuthoringControllerTest | **4 PASS** |
| ScenarioIndustryMigrationIntegrationTest | **1 PASS**, isolated V22→V25 |
| CorePackageAIntegrationTest#filteredCatalogueUsesStableCursorAndEnterpriseStatisticsNeverReturnPrivateWork | **1 PASS**, isolated fresh V1–V27 |
| Tổng backend | **14 PASS, 0 FAIL/ERROR/SKIP**, BUILD SUCCESS |
| `git diff --check`; `docker compose config --quiet` | **PASS** |
| GET gateway catalogue/bundle; catalogue API; 9 public details | **200 / 9 items / 9 detail thành công**, 0 restricted-field-name findings, không mutation |
| Full suites, fresh backend package, real Google | **NOT RUN / NOT RUN / BLOCKED**, không gán PASS |

Lệnh backend chính xác và frontend đầy đủ nằm trong evidence. Cả hai integration tests tự tạo disposable PostgreSQL qua Testcontainers; Core dùng DynamicPropertySource cho URL/user/password. Không dùng datasource CareerSim hiện tại. Test cursor Core tạo 3 scenarios synthetic, limit=1, đi hết 3 trang, kiểm tra không trùng/đủ 3 IDs và từ chối query thay đổi. Industry test kiểm tra SQL lọc trước pagination và cursor khác industry. **Cursor tự động PASS**, giữ TC-04-05 browser PARTIAL. Không thêm test UI mới: công cụ từ chối patch bổ sung; test có sẵn đủ kiểm tra cursor backend, không chứng minh click Next/First trong browser.

### D. Migration và integration

`origin/main=fdb85625191518135907122f86efd04aedfde1e3` khớp live `git ls-remote`; không fetch/ghi ref. Main có V1–V22, chưa có collision V23–V27. Các SQL V1–V22 không bị branch này sửa. V23–V27 duy nhất, liên tiếp; CRC32 source (UTF-8 từng dòng theo Flyway) khớp toàn bộ read-only database history, success=true:

| Version | Flyway CRC32 | SHA-256 file |
|---|---:|---|
| V23 | -680765044 | C6B2CA21EF82597AE91E8F30087558770C6216F85E8E4E6667B49ADADD7EF9C3 |
| V24 | 1570157704 | E72C9554FFFD10653B220744B52F97823DD5C5FCDA5210D858A8B43018C88920 |
| V25 | -583669794 | A841FA658C21FE1C01C00201BC99E37E89D0E2F91A63C8DBE826F1515DEE3199 |
| V26 | 1284167014 | F7C1558049CC1EBD51F8ABCD4C4348E9CE36C533BA50B712D66977EB2DDA7C6F |
| V27 | 743639303 | B99D7689E56DD5CE79ABC08DDB0B02CCA05915652C312252C568241FC9232449 |

Fresh V1–V27 PASS không tương đương test sửa dữ liệu populated V26/V27. Dedicated upgrade/mapping test dừng ở V25. V24–V26 tạm tắt guard version để sửa published media; V27 tắt guard task để sửa current published workplace_context, không cập nhật content_hash. Publish service hash chứa Draft(metadata,tasks), gồm workplaceContext: **rủi ro integrity cần PO/lead quyết định**. Không tự sửa migration đã áp dụng; nếu cần remediate phải là scope/SQL mới được duyệt và phối hợp version với team.

Main có thay đổi UC-03 cùng App.jsx imports/navigation (NavigationScroll thay ScrollToTop), CorePackageAIntegrationTest và alignment register. Cần reconcile để giữ cả navigation mới và aliases; không khẳng định merge sạch từ diff hiện tại. Không merge/rebase trong closure, không có test sau integration.

### E. Nội dung/media và hygiene

V24–V27 chứa URL ảnh/logo employer, Wistia embeds và tagline Forage. Không tìm thấy giấy phép tương thích/permission bằng văn bản trong thay đổi đã review. Nguồn [Forage terms](https://www.theforage.com/terms), kiểm tra ngày 2026-10-25, không cung cấp bằng chứng cho phép redistribution trong hồ sơ này. Quyền dùng local phục vụ học tập do PO duyệt không tự cấp quyền đưa nội dung lên repo/PR công khai/chia sẻ. **HOLD V24–V27 và 11 PNG** cho tới khi có quyết định quyền nội dung; không âm thầm bỏ SQL vì DAO mới cần schema V24. Giải pháp license-neutral hoặc migration tách riêng cần PO phê duyệt, không thực hiện ở đây.

11 PNG được xem contact sheet: không thấy credential/token/account detail rõ ràng; chưa chứng nhận metadata hoặc kiểm tra pháp lý. Các ảnh search/filter cũ còn text input; TC-04-04A ghi nhận dropdown cuối. Ảnh có logo, ảnh, đoạn mô tả và khung video bên thứ ba nên giữ riêng khỏi public staging. Không có file binary application mới trong diff.

Quét có phạm vi các text paths changed/untracked không thấy private-key/provider-key/JWT-token pattern. Không stage .env, auth/cookies/storageState, dump, dist, target hoặc log thô. `.env.example` đã có trong Git. AI_USAGE có thay đổi format bảng sẵn từ trước; không coi đó là feature mới, PO cần review trước stage. Không phải audit toàn bộ history/entropy.

### F. Tính mới của evidence và các gap

Build frontend đúng flags tạo `index-D9k6GUns.js`/`index-CBV6bjAl.css`, trùng cả tên và SHA-256 bytes với assets gateway đang phục vụ. Maven compile/test chạy trên source hiện tại. Fingerprint SHA-256 của manifest path/hash production inputs (không test, có package/build/config; backend có main resources) trong closure:

- Frontend: `E9E02294105D0D20CF5959D2013437F5C6B5242BF4331D199EA9DCB562878C0E`.
- Backend: `F599B50210A252BF8D2C65A01F72008F6A55B6741B6F537F71AD32E5E3C7CBBD`.

Không sửa application source trong closure. Container backend/frontend/gateway/PostgreSQL giữ nguyên ID/start time trong lượt kiểm tra. Hai worktree vẫn được giữ nguyên. Final commit SHA chưa có vì PO chưa duyệt Git write; HEAD chỉ định CP1, không đại diện toàn bộ dirty slice. Gaps: real Google, browser Next/First, populated V26/V27 correction regression, content rights/integrity decision, integration with main, final review/commit SHA. Survey được ghi D10 để review UC-05 sau UC-04, không triển khai.

### G. Whitelist staging chính xác và commit đề xuất — CHƯA THỰC HIỆN

Hiện **không stage gì**. Danh sách dưới là gói text/source dự kiến sau khi PO/lead giải quyết HOLD; không dùng `git add -A`. V23 đã commit, không nằm trong danh sách mới. Không loại V24–V27 rồi tuyên bố backend mới chạy được trên main.

Nhóm 1 — `feat(catalog): expose public media, task outlines and practiced skills` (**HOLD quyền nội dung/integrity**):

```text
backend/src/main/java/edu/hcmute/careersim/simulation/dao/ScenarioDao.java
backend/src/main/java/edu/hcmute/careersim/simulation/dto/ScenarioContracts.java
backend/src/main/java/edu/hcmute/careersim/simulation/service/impl/ScenarioAuthoringServiceImpl.java
backend/src/main/resources/db/migration/V24__add_public_scenario_media.sql
backend/src/main/resources/db/migration/V25__correct_public_scenario_media_slugs.sql
backend/src/main/resources/db/migration/V26__correct_remaining_public_scenario_media_slugs.sql
backend/src/main/resources/db/migration/V27__correct_imported_task_workplace_context.sql
backend/src/test/java/edu/hcmute/careersim/catalog/controller/VersionedCatalogControllerTest.java
backend/src/test/java/edu/hcmute/careersim/system/migration/CorePackageAIntegrationTest.java
backend/src/test/java/edu/hcmute/careersim/system/migration/ScenarioIndustryMigrationIntegrationTest.java
```

Nhóm 2 — `feat(catalog): complete public browse UI and safe login return`:

```text
deployment/nginx/default.conf
frontend/nginx.conf
frontend/src/app/App.jsx
frontend/src/features/identity/AccountPage.jsx
frontend/src/features/simulations/components/SimulationCatalog.jsx
frontend/src/features/simulations/pages/CoreCatalogPage.jsx
frontend/src/features/simulations/pages/CoreSimulationPage.jsx
frontend/src/features/simulations/pages/coreBrowse.test.jsx
frontend/src/shared/i18n/coreMessages.js
frontend/src/styles/index.css
```

Nhóm 3 — `docs(uc04): record PO UAT and final verification boundaries`:

```text
.agent/plans/US-04-UC-04-browse-simulations-checkpoint-1.md
.agent/reports/us-04-uc-04-browse-simulations-verification.vi.md
AI_USAGE.md
docs/evidence/README.md
docs/evidence/US-04/us-04-browse-simulations.md
docs/requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md
docs/requirements/traceability-matrix.md
docs/requirements/use-cases/UC-04-browse-simulations.md
```

PNG giữ local, không nằm whitelist: `UC04-01-catalogue-desktop.png`, `UC04-02-card-keyboard-focus.png`, `UC04-03-search-results.png`, `UC04-03-no-results.png`, `UC04-04-filter-sort-url.png`, `UC04-04A-career-track-dropdown.png`, `UC04-05-pagination-end-state.png`, `UC04-06-detail-header.png`, `UC04-06-detail-overview-tasks.png`, `UC04-07-task-detail.png`, `UC04-08-video-public.png` (tất cả trong `docs/evidence/US-04/`).

### H. Draft PR chuẩn bị để review, chưa publish

**NOT READY cho Draft PR public/shared**: cần giải quyết redistribution và integrity của correction migrations; review integration main. Các giới hạn Google/cursor browser được ghi rõ, không biến thành PASS. Không có Git write.

**Title:** `feat(catalog): complete UC-04 public simulation browsing`

**Proposed description (chưa phải xác nhận release):**

> CareerSim's public catalogue now supports server-side search/filter/sort/cursor pagination with URL state and an API-populated Career Track selector. Public detail exposes approved metadata, media, task outlines and practiced skills; unauthenticated Start returns through the existing login flow to the selected simulation. Cards use a single semantic link and responsive layout. Industry is fixed to IT. Recommendations, reviews, survey and simulation completion/results are excluded from this increment.
>
> Traceability: report-selected US-04 / UC-04, D09 public/no-recommendations decision; existing Core architecture and enrollment/authentication services preserved. CP1 V23 adds nullable IT classification without backfill. Proposed V24–V27 public-media/content corrections require explicit content-rights and published-version integrity clearance before sharing; do not merge this draft as-is.
>
> Verification on the current worktree: frontend 19 PASS, scoped lint/build PASS with Core enabled; backend focused 14 PASS including isolated PostgreSQL cursor traversal and public-field non-disclosure. Fresh V1–V27 and V22→V25 upgrade tests PASS; populated V26/V27 correction regression not covered. Compose config/diff checks PASS. Full suites were not rerun.
>
> PO-reported manual UAT, 2026-10-25: 14 PASS, 1 PARTIAL (browser next-page navigation not exercised with nine records), 1 BLOCKED (real Google disabled), 0 FAIL. Dropdown TC-04-04A passed all eight checks. Codex did not observe the full PO session. Survey absence is a UC-05 requirements-review item.
>
> Evidence: docs/evidence/US-04/us-04-browse-simulations.md and the existing Vietnamese verification report. Raw logs, credentials, database exports and unlicensed third-party screenshots are excluded. Final commit SHA, reviewer/lead sign-off, content permission and main integration remain pending. No valuable/shared database migration or application restart was performed during verification.

## Bổ sung UAT — 2026-10-25

PO duyệt đổi Career track thành dropdown có nhãn, lấy giá trị API public IT qua đủ trang, loại trùng và giữ URL state. Có All career tracks, loading/error/retry EN/VI; không đổi backend/database. Targeted Vitest UC-04 **19/19 PASS** ngoài sandbox (lượt sandbox bị EPERM cache trước khi chạy test); scoped ESLint và production build **PASS**. Sửa hai lỗi test cũ: đọc CSS bằng filesystem path, phân biệt reviews/ratings với nội dung self-review hợp lệ. Đây là kết quả mới bổ sung cho lịch sử BLOCKED bên dưới. Docker chưa refresh dropdown; PO browser UAT vẫn PENDING.

## 1. Phạm vi và nguồn yêu cầu

Feature này triển khai report-selected **US-04 Explore simulations** và **UC-04 Browse simulations** trên nhánh `feature/browse-simulations`. Nguồn yêu cầu là catalogue 10 US/16 UC hiện hành, Baseline v1 + DR1-DR2 cho các quy tắc bảo mật, quyết định PO trong phiên làm việc, và rubric `Final_Project_Assignment_2026-2027` mục 5.1, 5.3, 6 và 7.

Phạm vi đã duyệt gồm catalogue/detail công khai, search/filter/sort/cursor pagination phía server, Industry IT, trạng thái loading/empty/no-match/error/retry, giao diện responsive, và chuyển tiếp Start sang đăng nhập rồi quay lại simulation đã chọn. Recommendations, reviews, pre-enrollment survey, task completion và results không thuộc US-04.

Quyết định PO loại bỏ recommendations tạo khác biệt có chủ đích với UC-04 alternative 2a/2b. Catalogue vẫn hoạt động độc lập với career assessment.

## 2. Luồng đã triển khai

### Luồng chính

1. Người dùng mở `/simulations` hoặc đường dẫn tương thích `/simulation`.
2. React gửi truy vấn tới `GET /api/v2/scenarios?paged=true` với search, filter, sort, cursor và limit.
3. Backend chỉ truy vấn scenario `AVAILABLE` có version `PUBLISHED`; industry được lọc trong SQL trước pagination.
4. Catalogue hiển thị card compact, toàn card là một semantic link, có ảnh/company/title/tóm tắt hai dòng/role/duration/difficulty.
5. Detail hiển thị liên tục Overview, Video và Tasks; navigation anchor cuộn nhanh tới từng phần.
6. CTA Start yêu cầu xác thực. Khi chưa đăng nhập, frontend lưu một internal path đã giới hạn trong session storage, chuyển tới luồng account hiện hữu và quay lại detail sau đăng nhập.

### Luồng thay thế và ngoại lệ

- Search/filter/sort được lưu trong URL; thay đổi tiêu chí làm reset cursor.
- Empty catalogue và no-match là hai trạng thái riêng; no-match có Clear filters.
- Lỗi catalogue/detail có thông báo và retry phù hợp.
- Detail đã bị unpublish/inaccessible trả về trạng thái unavailable.
- Video chỉ nhúng URL Wistia HTTPS theo allowlist; ảnh và iframe chỉ được CSP cho phép từ CDN đã duyệt.
- Login-return từ chối absolute/external URL, stale hoặc malformed intent; enrollment tiếp tục dùng service hiện hữu để tránh logic tạo trùng ở frontend.

## 3. Sequence-to-code và design-to-code

| Bước | Thành phần thực thi | Mã nguồn chính |
|---|---|---|
| Mở catalogue và đọc URL state | React route/page | `frontend/src/app/App.jsx`, `frontend/src/features/simulations/pages/CoreCatalogPage.jsx` |
| Gọi API catalogue | Shared HTTP client | `frontend/src/shared/api/httpClient.js` |
| Validate query/cursor | Controller và catalog service | `backend/src/main/java/edu/hcmute/careersim/catalog/controller/VersionedCatalogController.java`, `backend/src/main/java/edu/hcmute/careersim/catalog/service/impl/VersionedCatalogServiceImpl.java` |
| Lọc published/available/industry trước pagination | DAO | `backend/src/main/java/edu/hcmute/careersim/simulation/dao/ScenarioDao.java` |
| Hiển thị lifecycle state/card | Existing catalogue component | `frontend/src/features/simulations/components/SimulationCatalog.jsx` |
| Mở public detail | Existing detail page | `frontend/src/features/simulations/pages/CoreSimulationPage.jsx` |
| Start và login-return | Existing detail/account pages | `frontend/src/features/simulations/pages/CoreSimulationPage.jsx`, `frontend/src/features/identity/AccountPage.jsx` |

Thiết kế tham khảo Forage chỉ được dùng cho hierarchy, card density, top CTA, anchor sections và task disclosure. Màu, typography, component/CSS, EN/VI và business rules tiếp tục dùng CareerSim. Không sao chép reviews, recommendation, certificate claim hoặc locked content.

## 4. API và database

### API công khai

- `GET /api/v2/scenarios?paged=true&q=&careerTrack=&difficulty=&industry=IT&sort=&cursor=&limit=`
- `GET /api/v2/scenarios/{slug}`

Public DTO bổ sung `industryCode`, public media, task outline mô tả công khai và tên practiced skills đã snapshot. Response không chứa model answer, rubric, evaluation key, private submission hay after-submission resource.

### Database

- V23 thêm nullable `scenario.industry_code VARCHAR(32)`, chỉ cho phép `NULL` hoặc `IT`, không default và không tự backfill.
- V24 thêm URL public media vào `scenario_version` với HTTPS/host constraints.
- V25-V26 sửa mapping slug media cho chín simulation đã được PO cho phép dùng cục bộ.
- V27 thay workplace-context token của task bằng public module tagline; ba tagline trống được giữ thành chuỗi rỗng.

V1-V22 không bị sửa. Trước V24 đã tạo backup local; database/volume PostgreSQL không bị reset hoặc recreate. Dữ liệu Forage chỉ được dùng trong môi trường học tập cá nhân theo xác nhận của PO; việc publish công khai vẫn cần review quyền sử dụng riêng.

## 5. Bảo mật, riêng tư và ranh giới AI

- Catalogue/detail công khai chỉ trả published metadata.
- Filter/pagination chạy server-side; frontend không dùng fixture fallback khi Core catalogue rỗng.
- Locked answers và private submissions không đi qua public DTO.
- Login-return chỉ chấp nhận đường dẫn nội bộ `/simulations/...`; absolute URL bị xóa để tránh open redirect.
- JWT/OAuth/account-linking không được thiết kế lại trong feature này.
- Feature không gọi LLM và không gửi dữ liệu người dùng tới provider. UI UX Pro Max và AI coding assistants chỉ hỗ trợ thiết kế/triển khai, không tham gia runtime.

## 6. Bằng chứng kiểm thử thực tế

| Kiểm tra | Kết quả quan sát |
|---|---|
| `.\mvnw.cmd -Dtest=VersionedCatalogControllerTest test` | PASS: 3 tests, 0 failures, 0 errors, 0 skipped |
| Scoped ESLint cho các file catalogue/detail/test/i18n | PASS: exit code 0 |
| `git diff --check` | PASS |
| Docker frontend `npm run build` | PASS: Vite production build, 1931 modules transformed |
| Docker backend package | BUILD SUCCESS, nhưng dùng `-DskipTests`; không được tính là test pass |
| Local Vitest trực tiếp | BLOCKED trước đó bởi sandbox `spawn EPERM`; test source có nhưng lượt cuối chưa thực thi |
| PostgreSQL/Testcontainers từ Manager sandbox | BLOCKED bởi Docker named-pipe access; không tuyên bố integration pass |

Runtime trước refinement cuối đã quan sát: 9 simulation, 4 card mỗi hàng desktop, 1 cột ở 320 px, 9 card images, 8 overview videos, không có “View simulation”, không overflow; API có 23 task descriptions không rỗng và 0 RSC token. Sau refinement cuối, backend/frontend/gateway build và start thành công, nhưng browser visual rerun bị Computer Use dừng vì không xác định chắc chắn URL cửa sổ Edge. PO browser UAT vẫn là điều kiện chấp nhận cuối.

## 7. Traceability acceptance criteria

| US-04 acceptance criterion | Implementation | Evidence/status |
|---|---|---|
| Browse without career quiz | Public routes and public v2 endpoints | Implemented; runtime/API observed |
| Search, filter, sort, pagination | Server query + URL state + cursor fingerprint including industry | Controller test PASS; PostgreSQL integration rerun pending |
| Detail shows role, expected tasks, public resources/metadata, difficulty | Public summary/detail DTO and continuous detail layout | Implemented; browser UAT after final refinement pending |
| Useful no-match state | Dedicated no-match state and Clear filters | Source/test present; Vitest execution blocked |
| No locked model answers | Public DTO allowlist and separate protected model-answer endpoint | Static/API response inspection passed; negative integration rerun pending |

## 8. Sai lệch và việc còn lại

- UC-04 source precondition nói authenticated, nhưng PO phê duyệt catalogue/detail public; authentication chỉ bắt đầu tại Start.
- UC-04 alternative recommendations bị loại khỏi Feature 1 theo quyết định PO.
- Chín simulation là dữ liệu local phục vụ học tập, chưa phải bằng chứng quyền publish công khai.
- Cần PO chạy browser UAT cuối trên desktop và 320 px, gồm keyboard, Back/refresh, EN/VI và login-return thường/Google OAuth.
- Cần chạy PostgreSQL integration test trên database test được xác nhận isolated trước khi chấp nhận migration/API đầy đủ.
- Working tree còn uncommitted; commit SHA cuối cho evidence phải được điền sau khi PO review và commit.

## 9. Artifact thay đổi

Mã chính tập trung trong catalog controller/service/DAO/DTO, migrations V23-V27, các trang/component simulations hiện hữu, account login-return tối thiểu, Nginx CSP, i18n, CSS và targeted tests. Không thêm UI framework, state-management framework hoặc dependency mới.
