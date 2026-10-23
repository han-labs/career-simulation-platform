# US-03 local demo và UAT

Chỉ dùng trên máy phát triển, không dùng cho production/shared deployment. Không
reset DB, không sửa Flyway V1–V22. Không cần Google hoặc triển khai Register Account.

## Chuẩn bị và tái tạo

Yêu cầu: Docker Desktop, Java 17, Maven wrapper của repo, Node và PowerShell 7.
Trong `.env` local bị Git ignore, dùng khóa JWT ngẫu nhiên hợp lệ hiện có và:

```dotenv
CORE_V2_ENABLED=true
VITE_CORE_V2_ENABLED=true
LEGACY_WRITE_ENABLED=false
```

Giữ nguyên các thiết lập khác. Không dùng khóa mẫu/khóa được đưa vào Git. Từ root:

```powershell
.\scripts\setup-us03-local.ps1
docker compose up -d --build
docker compose ps
```

Script chỉ chọn DB `career_simulation` trên `127.0.0.1` qua published port của
PostgreSQL Compose. Nó thêm COMPOSE_FILE/COMPOSE_PATH_SEPARATOR vào `.env` local
để chọn `docker-compose.us03-local.yml`; tất cả port chỉ bind loopback. Nếu có
override khác, script từ chối thay thế. Base Compose và production config không đổi.

Bootstrap nằm trong test source, chỉ chạy khi có flag RUN_LOCAL_US03_BOOTSTRAP=true,
profile local-us03 và JDBC loopback đúng DB. Script tự đặt rồi khôi phục các biến
process này; không cần thêm chúng vào `.env`. Bootstrap không được đóng gói vào JAR.
Nó không thêm HTTP endpoint, không bỏ auth/CSRF và không nhận user ID/role từ request.

Trong một transaction có advisory lock, script tạo publisher tổng hợp không có
password (không đăng nhập được) và hai Student tổng hợp qua cơ chế fixture hiện có.
Publication vẫn đi qua AssessmentAuthoringService và evaluator thật. Tài khoản thứ
hai chỉ phục vụ negative ownership tests. Không sửa/quyền hóa lại account trùng tên;
collision/password khác làm bootstrap thất bại và rollback.

Mỗi quiz có đúng một version trong bootstrap. Chạy lại chỉ xác minh content hash và
active pointer; không tạo type/version trùng, không sửa published version hoặc result.
Nếu đã có content khác hay lịch sử version do operator tạo thêm, script từ chối thay
vì tự suy đoán cách ghi đè. Không xóa file credentials để khắc phục collision.

## Nguồn nội dung và scoring

| Type | Câu / nhóm | Evaluator | Khoảng điểm mỗi nhóm |
|---|---|---|---|
| RIASEC | 42 / R,I,A,S,E,C (7 mỗi nhóm) | riasec-sum/1 | 7–35 |
| Interests | 12 / Analytical, Creative, Collaborative, Organizing | profile-sum/1 | 3–15 |
| Abilities | 12 / Problem solving, Communication, Organization, Technical learning | profile-sum/1 | 3–15 |
| Work Activities | 9 / Information, People, Things | profile-sum/1 | 3–15 |

Câu EN lấy từ inventory project hiện có: V4__update_riasec_questions_to_42.sql,
V10__add_interests_and_abilities_quizzes.sql, V11__add_work_activities_quiz.sql.
Không sửa các file này. Dịch RIASEC tái sử dụng contentMessages; 33 bản dịch profile
bổ sung trong assessmentMessages. Bootstrap yêu cầu đủ EN/VI và đúng số câu/category,
required LIKERT 1–5, rồi để evaluator hiện có validate trước publish. Đây là self-report
để khám phá trong local demo, không phải test tâm lý/aptitude đã được chứng nhận.
Không tải hoặc sao chép nội dung bên thứ ba trong nhiệm vụ này. Rà soát ngôn ngữ/
psychometric/học thuật của con người là bước riêng, không suy diễn từ test kỹ thuật.

## Identity và các bước nghiệm thu chính xác

Mở http://localhost:8080/account. Chỉ dùng form Sign in hiện có, không Register/Google.
Lấy identity tổng hợp trên chính máy local:

```powershell
Get-Content .\tmp\us03-demo-credentials.json
```

Email: `us03-student@local.example.test`; password được sinh ngẫu nhiên trong file
ignored trên. Không gửi password vào chat/Git/screenshot. Tài khoản và các kết quả
smoke được giữ lại; history không bị xóa sau mỗi test.

1. Mở http://localhost:8080/assessment. Kiểm tra dropdown có đủ bốn quiz. Chọn bất
   kỳ bài nào; không cần hoàn thành RIASEC trước. Link Explore IT simulations độc lập.
2. Chọn Start or resume. Nếu đã có kết quả từ lần test trước, chọn Retake assessment
   (RIASEC) hoặc Start another attempt rồi Start or resume để tạo lần làm mới.
3. Đọc hướng dẫn. Xác nhận 42/12/12/9 câu đúng loại; đổi Website language để xem EN/VI.
4. Submit Answers khi chưa trả lời: phải thấy thông báo required, lỗi gần câu hỏi,
   focus câu thiếu; không có completed result mới.
5. Trả lời một phần → Save draft → chờ Saved on the server → F5 → Resume draft trong
   history. Câu trả lời phải còn. Postpone assessment trở về selection, không tạo result.
6. Trả lời đủ → Submit Answers. Chọn toàn mức 5 sẽ có RIASEC 35/35 (7–35) hoặc từng
   category profile 15/15 (3–15). Đây là điểm server trả, không phải kết quả frontend giả.
7. F5 → View saved result trong history: cùng attempt/version/điểm. Làm các bài khác
   hoặc retake; các kết quả cũ phải còn riêng. Toàn mức 3 cho profile tạo điểm 9/15.
8. Desktop và mobile 320px: headings/cards/radio labels không cuộn ngang; field errors,
   focus và nút bấm dùng được. Không cần consent AI để làm quiz.
9. Sau restart backend, CSRF session cũ có thể không còn. Reload/Resume draft để lấy
   phiên CSRF mới rồi Submit; dữ liệu chưa được server chấp nhận không được coi completed.

## Kiểm tra API thật và persistence

```powershell
node scripts/verify-us03-local.mjs
# Khi chủ động kiểm tra restart ở local: giữ volume, KHÔNG dùng down -v.
docker compose restart postgres backend
# Chờ /api/health trở lại 200 rồi:
node scripts/verify-us03-local.mjs --recheck
```

API suite dùng HTTP thật qua gateway, cookies/CSRF thật và nhịp 600ms để tuân thủ
rate limit. Nó tạo các attempt test mới, kiểm tra required/invalid, revision conflict,
idempotency, retake, owner isolation và unavailable version 404; snapshot không có
credentials nằm trong tmp/us03-api-evidence.json. `--recheck` chỉ đọc kết quả đã lưu.
Không tự inject SQL lỗi hoặc sửa published content trong DB local để test exception.

Quality gates:

```powershell
cd backend
.\mvnw.cmd verify
cd ..\frontend
npm run check
cd ..
docker compose config --quiet
```

Bootstrap được skip có chủ đích trong gate bình thường; guard tests vẫn chạy.
Xem [bằng chứng AC1–AC5](../../.agent/reports/us-03-local-acceptance-2026-10-23.vi.md).
