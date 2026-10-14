# CareerSim — Sửa xung đột phiên bản Flyway sau merge main

Ngày: 14/10/2026. Branch: `codex/package-a-core`.
HEAD: `ce43c55d5786ce84193ac66f715b89de075aa927`; thay đổi chưa commit.
Working tree trước tác vụ: sạch. Không commit, push hoặc deploy.

## Phạm vi và quyết định

Đã đọc AGENTS và chuỗi `.agent`, rules liên quan, toàn bộ 18 migration SQL,
tham chiếu trong code/tests/tài liệu và năm specification Astra.
Merge tạo hai cặp trùng: V13 identity/Syn knowledge, V14 assessment/Syn trace.
Phương án V14–V17 vẫn trùng V14; đã báo cáo trước khi sửa. Người dùng trả lời
"continue" sau đề xuất Package A V15–V18 / Package B V19–V21.

Chỉ đổi tên bốn file Package A, cập nhật preflight, tests và tài liệu liên quan.
Không thay đổi API, frontend source, nghiệp vụ, module boundary hay nội dung SQL.
Package B chưa được triển khai; không tạo SQL V19–V21.

| Nội dung | Số cũ | Số hiện tại |
|---|---|---|
| Identity / enterprise / consent foundation | V13 | V15 |
| Versioned assessment / assets | V14 | V16 |
| Versioned simulation / completion | V15 | V17 |
| Confirmed exploration plan | V16 | V18 |
| Agent registry / knowledge, dự kiến | V17 | V19 |
| Agent runtime / approvals, dự kiến | V18 | V20 |
| Integrity guards, dự kiến | V19 | V21 |

Main/Syn V1–V14 giữ nguyên tên và bytes. SHA-256 trước/sau: **18/18 khớp**,
bao gồm bốn phép đổi tên. Source và JAR clean build đều có **18 phiên bản duy nhất**.

| File hiện tại | SHA-256 |
|---|---|
| V13__syn_vector_knowledge.sql | 5B9A26A91A3811EF7E569A9288E32DFD7055D33F470A240B9DBF9F1FC6E0927F |
| V14__syn_turn_trace.sql | A5AA17D055901DC56F6D0A7F49443FFCE709E44291FD5749674B1B6DE7231976 |
| V15__identity_enterprise_consent_foundation.sql | A40519A50BA84B8522F3BAA09B07C4FD8BCDB25F2B92A0FCA31CFEDE2E0B8A5D |
| V16__versioned_assessment_and_assets.sql | 46881035DD5314793A38BF06756BE7721B1FBFD201DE855F1906D3C9CF5FECF9 |
| V17__versioned_simulation_and_completion.sql | 2701896370EA20C0242DC0E989E72B37467EE87673943EA4AB7F803312C34341 |
| V18__confirmed_exploration_plan.sql | 1AAF5C915686E0EFE2B91FFFC782E04F2C2BBA9EE0B8F548EC31C670D72BF68A |

Comment trong SQL V18 vẫn ghi "Package B/V18" theo numbering gốc. FK proposal-plan
thuộc migration dự kiến V20 hiện tại; đã giải thích trong docs/database/README.md
và cập nhật specification, không sửa comment để giữ nguyên SQL/hash.

## Thay đổi code và bằng chứng database cô lập

- `MigrationPreflightService.java:37`: head bắt buộc V18, giữ nguyên kiểm tra
  database/schema/reviewer/hash và dry-run.
- `MigrationInventoryTest.java:15`: kiểm tra phiên bản Flyway duy nhất trên
  classpath, bao gồm trường hợp resources cũ còn trong build output.
- `SchemaUpgradeIntegrationTest.java:71`: baseline main V14 → V18 áp dụng đúng
  bốn migrations; giữ version/script/checksum của 14 migrations đã áp dụng;
  rerun không áp dụng thêm migration; inventory chính xác 49 bảng.
- Fingerprint 24 bảng nguồn, gồm ba `career_quiz_*` của main, được đối chiếu nguyên
  vẹn. Fixture có lịch sử quiz, Syn response/trace và published knowledge/vector
  tổng hợp để kiểm tra preservation; không gọi embedding provider thật.
- `SchemaUpgradeIntegrationTest.java:17`: email collision chặn V15, transaction
  rollback, hai nguồn giữ nguyên, chưa tạo bảng enterprise.
- `CorePackageAIntegrationTest.java:677`: dump/restore sang database test khác
  kiểm tra head V18, submission/snippet/FK và write mới; gateway core flow vẫn qua.

Tất cả database nêu trên do Testcontainers tạo mới trên PG17/pgvector 0.8.2.
Không kết nối database có giá trị; không Flyway repair/baseline/clean, reset history
hay xóa dữ liệu thật. Maven `clean` chỉ xóa build output của backend.

## Commands đã chạy và kết quả

| Kiểm tra | Kết quả |
|---|---|
| Trước sửa: `.\mvnw.cmd -q clean verify` | FAIL: 195 tests, 40 errors do trùng V13, 4 skips |
| Focused test sau đổi tên nhưng chưa clean | FAIL: phát hiện SQL cũ còn trong target/classes; xử lý bằng Maven clean |
| `.\mvnw.cmd -q clean "-Dtest=MigrationInventoryTest,SchemaUpgradeIntegrationTest" test` | PASS: 3 tests |
| Cuối: `.\mvnw.cmd -q clean verify` | PASS: 34 suites, 196 discovered, 192 passed, 4 skipped, 0 failures/errors; Spotless, JAR và JaCoCo qua |
| Frontend: `$env:VITE_CORE_V2_ENABLED='true'; npm run check` | PASS: 76 tests / 16 files; lint và production build qua |
| `docker compose config --quiet` | PASS |
| Source inventory / SHA-256 trước-sau / JAR ZIP inventory | PASS: 18 phiên bản, không trùng; 18 SQL hashes giữ nguyên |
| `git diff --check` | PASS |

Bốn skips là live Gemini/OpenAI opt-in; không dùng skips để bỏ qua PostgreSQL.
Hai lần thử tham số scope cho Spotless không hợp lệ đã sửa; lần chạy giới hạn đúng
hai file test và final Spotless check qua. Lệnh `jar` không có trên PATH; đã đọc
JAR bằng .NET ZipFile thay thế, không cài tool/dependency.

## Tài liệu, giới hạn và tái lập

Cập nhật năm Astra docs, architecture/database README, task/current Package A plan,
và note ở báo cáo/log cũ. Baseline JSON và các kết quả test trước merge vẫn giữ
nguyên như bằng chứng lịch sử; chỉ sửa tên artifact SQL và chỉ dẫn Package B hiện tại.
Inventory được bổ sung ba bảng quiz của main: 24 legacy + 25 Package A = 49;
full target sau Package B dự kiến 24 + 40 = 64, không thêm yêu cầu domain mới.
Các ERD/UML có sẵn và frontend source không thay đổi.

Việc Package A từng được áp dụng vào shared/valuable database: **NOT VERIFIED**.
Repository/chats chỉ ghi nhận Package A testing cô lập, chưa cutover. Nếu runtime
thực tế có các script Package A mang số cũ, dừng rollout và báo cáo; không repair
hay đổi history tự động. Revised SRS vẫn thiếu; không suy ra yêu cầu mới.

Tái lập: trên Java 17/Node/Docker, chạy frontend check với core flag để có dist cho
gateway test, rồi backend `clean verify`, Compose config và diff check. Không cần
khởi chạy application với datasource mặc định hoặc truy cập database hiện hữu.
