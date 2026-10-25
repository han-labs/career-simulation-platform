# Chẩn đoán runtime — 2026-10-23

HEAD: `62bbe1012e9e5c03dad1719b84cd40fff5fce499`, kèm thay đổi UC-03 chưa commit
đã tồn tại trước yêu cầu này và được giữ nguyên. Không sửa application source.

## Nguyên nhân và sửa chữa

- Service thực tế: postgres/backend/frontend/gateway. Ban đầu `compose ps` không có
  service running; log lịch sử không được coi là trạng thái hiện tại.
- Sau `docker compose up -d --build`, backend restart; API gateway 502, frontend 200.
  Log: `BrowserAuthenticationImpl: Constructor threw exception`, nguyên nhân cuối
  `IllegalStateException: Set JWT_SIGNING_KEY to at least 32 random bytes (base64).`
- Tạo khóa ngẫu nhiên 48 byte, base64 trong `.env` local bị Git ignore; không in khóa,
  không dùng ephemeral/bỏ validation, không đổi cookie security/authentication logic.
- Ban đầu Core/Agent/legacy writes đều false. Người dùng phê duyệt bật đồng bộ
  CORE_V2_ENABLED, VITE_CORE_V2_ENABLED, AGENT_PLATFORM_ENABLED và
  VITE_AGENT_PLATFORM_ENABLED tại local. Chỉ sửa bốn cờ này và khóa JWT trong `.env`.
  Legacy writes vẫn false. Rebuild bằng `docker compose up -d --build`.
- Các route v2 catalogue trước khi bật Core trả 500 do route không được đăng ký;
  sau bật đúng workflow đều 200. Không sửa shared exception handler trong nhiệm vụ này.

## Kết quả thực thi

| Kiểm tra | Kết quả |
|---|---|
| Compose build/start, ps | PASS: cả 4 service running; postgres healthy |
| RestartCount backend/frontend/gateway | PASS: đều 0 sau rebuild cuối |
| PostgreSQL pg_isready | PASS: accepting connections |
| Flyway | PASS: validated 22 migrations; schema public v22 up to date, no migration necessary |
| Gateway `/`, `/api/health`, `/api/v2/auth/providers` | 200 |
| `/api/v2/assessment-types`, `/api/v2/scenarios` sau bật Core | 200; cả hai danh sách rỗng |
| `/api/v2/me` khi chưa đăng nhập | 401 đúng contract |
| `backend/.\mvnw.cmd verify` | PASS: tổng 330, 325 pass, 0 fail/error, 5 live skip; gồm PostgreSQL/Core/Auth/Agent/upgrade/gateway fixtures cô lập |
| `frontend/npm run check` | PASS: 115 pass, 0 fail/skip, lint/build thành công |
| `docker compose config --quiet`, `git diff --check` | PASS |
| Git diff migrations so HEAD | Rỗng; không chỉnh V1–V22 |

Browser thật tại http://localhost:8080: Landing tải catalogue legacy từ backend;
đăng ký tài khoản tổng hợp với display name Synthetic Runtime Smoke thành công,
đăng nhập hiện STUDENT và consent chưa chọn, Dashboard hiện các section, Assessment
hiện đúng trạng thái chưa có phiên bản reviewed/published, Agents hiện chưa được
gán Agent. Đăng xuất trở về form Sign in. Tài khoản smoke tổng hợp được giữ lại,
không xóa record; không ghi consent hoặc gọi AI. Không sửa/xóa dữ liệu cũ.

## Giới hạn

Core catalogue rỗng là thiếu nội dung published, không phải lỗi startup. Nộp quiz,
hoàn thành simulation và Agent conversation đã có regression bằng fixtures trên
PostgreSQL cô lập; không tuyên bố các luồng đó đã qua browser trên DB local hiện tại.
Cần Admin rà soát/publish nội dung, assign Agent và consent hợp lệ; không tự tạo dữ
liệu kinh doanh hoặc publish nội dung ngoài phạm vi sửa runtime. Google/live AI và
5 live test chưa chạy. Shared handler trả 500 với route bị tắt vẫn là hạn chế riêng.

Không commit/push/merge/deploy, không reset/drop/delete volume/database. Thay đổi
riêng của nhiệm vụ này: `.env` ignored, báo cáo này và AI-assistance log tương ứng.
Raw `.runtime-*.log` bị ignore, không đưa vào Git hoặc chia sẻ credentials.
