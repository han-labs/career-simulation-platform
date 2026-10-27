# Bốn Career Assessments trên stack local hiện tại

Chỉ dùng fixture local do operator chủ động chạy; không phải seed production.
Yêu cầu Docker Desktop, Java 17, Maven wrapper, Node và PowerShell 7.

Từ root của checkout đang phát triển, giữ nguyên `.env` và các volume hiện có:

```powershell
docker compose -p careersimfresh -f docker-compose.yml build backend frontend
docker compose -p careersimfresh -f docker-compose.yml up -d --no-build --pull never
.\scripts\setup-us03-local.ps1 -EnableLocalBootstrap
```

Script chỉ dùng base Compose và project `careersimfresh`; không đọc lựa chọn stack
cũ từ COMPOSE_FILE, không dùng `docker-compose.us03-local.yml`, không tạo stack mới.
PostgreSQL phải đang chạy. Script đọc tên database thực tế và mapped port, xác nhận
Compose labels/database backend, chỉ kết nối JDBC qua 127.0.0.1 và đối chiếu server ID.
Nó từ chối Docker remote, tên DB/target không khớp, schema chưa cập nhật hoặc thiếu opt-in.
Không sửa `.env`, không sinh credentials, không reset DB/volume, không Flyway clean/repair.

Backend mới chạy các migration hiện có đến V28 trong startup bình thường. Fixture
không chạy migration và không có V29. Biến process tạm thời được khôi phục sau chạy.
Output Maven ở `tmp/assessment-bootstrap.log` bị Git ignore; không chia sẻ raw logs.

Fixture test-source tái sử dụng AssessmentAuthoringService và publisher local không
có password. Không tạo Student. Một transaction + advisory lock bao toàn bộ publish.
Chạy lại chỉ xác nhận inventory/hash/status/activeVersionId; nội dung khác sẽ bị từ chối,
không ghi đè published version hoặc kết quả của người dùng.

| Assessment | Số câu | Evaluator |
|---|---:|---|
| RIASEC | 42 | riasec-sum/1 |
| Interests | 12 | profile-sum/1 |
| Abilities | 12 | profile-sum/1 |
| Work Activities | 9 | profile-sum/1 |

Nội dung EN và bản dịch VI tái sử dụng inventory/dictionary đã có, không tải nội dung mới.
Đây là nội dung self-report local phục vụ khám phá; không chứng nhận năng lực/tâm lý.
Auth, UC-07 và simulation không bị fixture cập nhật.

## Runtime local hiện hành (2026-10-27)

- `local` bình thường dùng xác thực thật: anonymous vẫn anonymous, đăng nhập đúng role,
  logout thu hồi credential. `LOCAL_DEMO_MODE=false` là mặc định; không cần auth-test.
- Shortcut lập trình viên chỉ bật với `LOCAL_DEMO_MODE=true` cùng profile local;
  prod/production luôn tắt shortcut. Không bật nó khi nghiệm thu xác thực thật.
- Không sửa `.env` chỉ để bootstrap/đổi profile. Cookie HTTP có cấu hình riêng cho local;
  không áp dụng ngoại lệ này cho production. Giữ cùng localhost:8080 trong cả phiên.

```powershell
docker compose -p careersimfresh -f docker-compose.yml up -d --no-build --pull never
```

## UAT bằng tài khoản Student thật

1. Mở `/account`, đăng ký Student mới bằng form hiện có rồi đăng nhập.
2. `/api/v2/assessment-types` phải trả đúng bốn code RIASEC, INTERESTS, ABILITIES,
   WORK_ACTIVITIES với activeVersionId. UI `/assessment` có bốn thẻ tương ứng.
3. Mở hướng dẫn một bài, Start or resume và xác nhận các câu hỏi thật được tải.
4. Student không có link Admin Studio. Sign out trong header trả `/account`;
   đăng nhập lại rồi mở `/simulations`, detail và completion/results theo PR #10.
5. Kiểm tra UC-07 cũ vẫn còn sau bootstrap; không xóa volume để xử lý lỗi.

Không in hoặc đưa password/token/cookie/API key vào terminal, screenshot hay báo cáo.
Google OAuth và AI live là các acceptance riêng, không cần để chạy bootstrap này.
