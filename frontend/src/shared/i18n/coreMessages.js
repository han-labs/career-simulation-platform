// Package A presentation strings. Persisted content translations remain version-pinned.
export const coreMessages = Object.fromEntries(`
Account and privacy ⇢ Tài khoản và quyền riêng tư
Sign in ⇢ Đăng nhập
Sign out ⇢ Đăng xuất
Register ⇢ Đăng ký
Email ⇢ Địa chỉ email
Password ⇢ Mật khẩu
Display name ⇢ Tên hiển thị
Role ⇢ Vai trò
Student ⇢ Sinh viên
Enterprise ⇢ Doanh nghiệp
Enterprise name (enterprise accounts only) ⇢ Tên doanh nghiệp (chỉ dành cho tài khoản doanh nghiệp)
Account created. Sign in to continue. ⇢ Đã tạo tài khoản. Đăng nhập để tiếp tục.
Purpose-specific consent ⇢ Đồng ý theo từng mục đích
Consent is optional. Assessment and simulations work without external AI consent. Revoking consent blocks future access; it cannot recall data already delivered. ⇢ Sự đồng ý là tùy chọn. Bạn vẫn làm đánh giá và mô phỏng khi không đồng ý xử lý AI bên ngoài. Thu hồi sẽ chặn truy cập tiếp theo; không thể thu hồi dữ liệu đã gửi.
Active ⇢ Đang có hiệu lực
Revoked or expired ⇢ Đã thu hồi hoặc hết hạn
Revoke ⇢ Thu hồi
Purpose ⇢ Mục đích
External AI processing ⇢ Xử lý bằng AI bên ngoài
Share profile ⇢ Chia sẻ hồ sơ
Share completions ⇢ Chia sẻ việc hoàn thành mô phỏng
Share assessment results ⇢ Chia sẻ kết quả đánh giá
Share submissions with their owner enterprise ⇢ Chia sẻ bài nộp với doanh nghiệp sở hữu mô phỏng
AI provider ⇢ Nhà cung cấp AI
Recipient enterprise ID (sharing only) ⇢ Mã doanh nghiệp nhận dữ liệu (chỉ khi chia sẻ)
I explicitly confirm this purpose and recipient under policy careersim-purpose-v1. ⇢ Tôi xác nhận rõ mục đích và bên nhận này theo chính sách careersim-purpose-v1.
Confirm consent ⇢ Xác nhận đồng ý
Your older browser draft is preserved separately. New attempts use a fixed content version. ⇢ Bản nháp cũ trong trình duyệt được giữ riêng. Lượt làm mới sử dụng một phiên bản nội dung cố định.
Assessment type ⇢ Loại đánh giá
Start or resume ⇢ Bắt đầu hoặc tiếp tục
No reviewed assessment version is published yet. ⇢ Chưa có phiên bản đánh giá đã rà soát được công bố.
Version ⇢ Phiên bản
Reload saved work ⇢ Tải lại bài đã lưu
Save draft ⇢ Lưu bản nháp
Local draft. Save to keep it on the server. ⇢ Bản nháp trên máy. Hãy lưu để giữ trên máy chủ.
Saved on the server. ⇢ Đã lưu trên máy chủ.
Your draft is preserved. Reload the attempt before retrying a conflict. ⇢ Bản nháp của bạn được giữ lại. Tải lại bài trước khi thử xử lý xung đột.
Final submission cannot be changed. Submit this task? ⇢ Bài nộp cuối không thể thay đổi. Bạn muốn nộp nhiệm vụ này?
Enroll in this version ⇢ Tham gia phiên bản này
required tasks submitted ⇢ nhiệm vụ bắt buộc đã nộp
Task ⇢ Nhiệm vụ
Final submission ⇢ Bài nộp cuối
Deliverables ⇢ Sản phẩm cần nộp
(optional) ⇢ (tùy chọn)
File uploaded ⇢ Đã tải tệp lên
No file selected ⇢ Chưa chọn tệp
Remove optional item ⇢ Bỏ mục tùy chọn
Submit final task ⇢ Nộp nhiệm vụ lần cuối
View model answer ⇢ Xem đáp án mẫu
Model answer ⇢ Đáp án mẫu
No model answer was provided. ⇢ Không có đáp án mẫu cho nhiệm vụ này.
Simulation completed ⇢ Đã hoàn thành mô phỏng
This records simulation participation, not professional competence. ⇢ Bản ghi xác nhận việc tham gia mô phỏng, không chứng nhận năng lực chuyên môn.
Fixed resume snippet ⇢ Đoạn mô tả CV cố định
View certificate ⇢ Xem giấy xác nhận
Dashboard ⇢ Bảng tiến độ
Assessment results ⇢ Kết quả đánh giá
Enrollments ⇢ Các mô phỏng đang tham gia
Practiced skills ⇢ Kỹ năng đã thực hành
Completions ⇢ Các mô phỏng đã hoàn thành
Legacy evaluations (historical scores) ⇢ Đánh giá cũ (điểm lịch sử)
Content version unknown ⇢ Không xác định phiên bản nội dung
Plan title ⇢ Tên kế hoạch
Exploration plan ⇢ Kế hoạch khám phá
One to five steps, one per line ⇢ Từ một đến năm bước, mỗi dòng một bước
Confirm and save plan ⇢ Xác nhận và lưu kế hoạch
Unable to establish a secure session. ⇢ Không thiết lập được phiên an toàn.
Sign in is required. ⇢ Vui lòng đăng nhập.
Finalize this task before viewing its model answer. ⇢ Nộp nhiệm vụ này trước khi xem đáp án mẫu.
All required deliverables must be supplied. ⇢ Cần cung cấp đủ sản phẩm bắt buộc.
The draft changed. Reload before saving. ⇢ Bản nháp đã thay đổi. Hãy tải lại trước khi lưu.
The request requires an authenticated session. ⇢ Yêu cầu cần một phiên đăng nhập.
No simulations are published yet. ⇢ Chưa có mô phỏng nào được công bố.
`.trim().split('\n').map(line => line.split(' ⇢ ')))
