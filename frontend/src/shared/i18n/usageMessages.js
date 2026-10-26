export const usageMessages = Object.fromEntries(`
AI Usage ⇢ Sử dụng AI
Monitor AI request volume, runtime health, latency and cost. ⇢ Theo dõi số yêu cầu AI, tình trạng vận hành, độ trễ và chi phí.
AI usage filters ⇢ Bộ lọc sử dụng AI
Time range ⇢ Khoảng thời gian
Last 24 hours ⇢ 24 giờ qua
Last 7 days ⇢ 7 ngày qua
Last 30 days ⇢ 30 ngày qua
Last 366 days ⇢ 366 ngày qua
Agent ⇢ Trợ lý
All Agents ⇢ Tất cả trợ lý
Provider ⇢ Nhà cung cấp
All providers ⇢ Tất cả nhà cung cấp
Model ⇢ Mô hình
All models ⇢ Tất cả mô hình
Outcome ⇢ Kết quả
All outcomes ⇢ Tất cả kết quả
Apply filters ⇢ Áp dụng bộ lọc
Apply filters to update results. ⇢ Áp dụng bộ lọc để cập nhật kết quả.
Filter choices are limited to 100. Narrow the time range for other choices. ⇢ Danh sách bộ lọc giới hạn 100 mục. Thu hẹp khoảng thời gian để xem lựa chọn khác.
Agent Platform executions only: Student Chat, Admin Preview and KB Test. Legacy Syn and ingestion are excluded. ⇢ Chỉ gồm lượt thực thi của nền tảng Agent: trò chuyện sinh viên, xem trước của quản trị viên và kiểm thử kho tri thức. Không gồm Syn cũ và nhập tài liệu.
Loading AI usage... ⇢ Đang tải dữ liệu sử dụng AI...
AI usage unavailable ⇢ Không tải được dữ liệu sử dụng AI
AI usage data could not be loaded. Please try again. ⇢ Không thể tải dữ liệu sử dụng AI. Vui lòng thử lại.
Administrator access is required. Sign in with an authorized account. ⇢ Yêu cầu quyền quản trị viên. Đăng nhập bằng tài khoản được cấp quyền.
Retry ⇢ Thử lại
Applied reporting interval ⇢ Khoảng báo cáo đã áp dụng
Historical Agent ⇢ Trợ lý trong lịch sử
AI usage key metrics ⇢ Chỉ số sử dụng AI chính
Requests ⇢ Yêu cầu
One persisted execution per request, including no-model requests. ⇢ Mỗi yêu cầu là một lượt thực thi đã lưu, kể cả yêu cầu không gọi mô hình.
Estimated Cost ⇢ Chi phí ước tính
Recorded generation subtotal in USD; not provider billing. ⇢ Tổng phụ chi phí sinh nội dung đã ghi nhận bằng USD; không phải hóa đơn nhà cung cấp.
unavailable ⇢ chưa có dữ liệu
partial ⇢ một phần
not applicable ⇢ không áp dụng
Average Latency ⇢ Độ trễ trung bình
Not available ⇢ Chưa có dữ liệu
measured finished executions ⇢ lượt đã hoàn tất có số đo
missing measurements ⇢ lượt thiếu số đo
Failed / Refused ⇢ Lỗi / Từ chối
Failed executions / refused executions. Refusal is not an infrastructure failure. ⇢ Lượt bị lỗi / lượt bị từ chối. Từ chối không đồng nghĩa lỗi hạ tầng.
No AI usage records are available for this period. ⇢ Chưa có bản ghi sử dụng AI trong khoảng thời gian này.
No executions match the selected filters. Change the filters or reporting interval. ⇢ Không có lượt thực thi phù hợp. Hãy đổi bộ lọc hoặc khoảng báo cáo.
Outcome health ⇢ Tình trạng kết quả thực thi
Execution outcomes retain their original meaning. ⇢ Giữ nguyên ý nghĩa các trạng thái thực thi.
Succeeded ⇢ Thành công
Failed ⇢ Lỗi
Refused ⇢ Từ chối
Cancelled ⇢ Đã hủy
Accepted ⇢ Đã tiếp nhận
Running ⇢ Đang chạy
In progress ⇢ Đang xử lý
Execution pagination ⇢ Phân trang lượt thực thi
Previous page ⇢ Trang trước
Next page ⇢ Trang sau
Page ⇢ Trang
Latency is full execution latency in milliseconds, measured for finished executions only. Unknown cost is never zero; partial cost is not a complete total. Embedding costs are excluded. ⇢ Độ trễ của toàn bộ lượt thực thi tính bằng mili giây, chỉ đo cho lượt đã hoàn tất. Chi phí chưa rõ không phải bằng không; chi phí một phần chưa phải tổng đầy đủ. Không gồm chi phí embedding.
Request volume over time ⇢ Số yêu cầu theo thời gian
Hourly buckets, UTC. Only buckets with requests are shown. ⇢ Nhóm theo giờ UTC. Chỉ hiển thị nhóm có yêu cầu.
Daily buckets, UTC. Only buckets with requests are shown. ⇢ Nhóm theo ngày UTC. Chỉ hiển thị nhóm có yêu cầu.
Request volume chart. Exact values are available below. ⇢ Biểu đồ số yêu cầu. Số liệu chính xác có ở bên dưới.
View trend values ⇢ Xem số liệu theo thời gian
Requests per UTC bucket ⇢ Yêu cầu theo nhóm thời gian UTC
Time ⇢ Thời gian
Recent AI executions ⇢ Lượt thực thi AI gần đây
Operational metadata only. No Student identity, messages, prompts or tool arguments. ⇢ Chỉ có thông tin vận hành. Không có danh tính sinh viên, tin nhắn, prompt hoặc tham số công cụ.
No executions remain on this page. Return to the previous page or apply filters again. ⇢ Trang này không còn lượt thực thi. Quay lại trang trước hoặc áp dụng lại bộ lọc.
Recent executions, newest first (UTC) ⇢ Lượt thực thi gần đây, mới nhất trước (UTC)
Tools ⇢ Công cụ
Latency ⇢ Độ trễ
Tokens ⇢ Số token
Cost ⇢ Chi phí
Knowledge Base test ⇢ Kiểm thử kho tri thức
Details ⇢ Chi tiết
Execution ID ⇢ Mã lượt thực thi
Execution mode ⇢ Chế độ thực thi
Configuration ID ⇢ Mã cấu hình
Not applicable ⇢ Không áp dụng
Configuration version ⇢ Phiên bản cấu hình
Prompt fingerprint ⇢ Dấu vân tay prompt
Recorded model attempts ⇢ Số lần gọi mô hình đã ghi nhận
Server retrieval calls ⇢ Lượt truy xuất của máy chủ
Failure or refusal category ⇢ Nhóm lỗi hoặc lý do từ chối
Other ⇢ Khác
No answer model ⇢ Không gọi mô hình trả lời
native attempts ⇢ lượt gọi công cụ
server retrieval ⇢ lượt truy xuất máy chủ
In ⇢ Đầu vào
Out ⇢ Đầu ra
Partial estimate ⇢ Ước tính một phần
Estimated ⇢ Ước tính
estimated ⇢ có ước tính
No model call recorded ⇢ Chưa ghi nhận lần gọi mô hình
Model identifies the selected model. Recorded model attempts describe actual calls; a selection alone does not imply a provider call. ⇢ Mô hình thể hiện lựa chọn của yêu cầu. Số lần gọi đã ghi nhận thể hiện hoạt động thực tế; chọn mô hình không có nghĩa đã gọi nhà cung cấp.
Unknown ⇢ Chưa rõ
Filters apply to the entire dashboard. ⇢ Bộ lọc áp dụng cho toàn dashboard.
Incomplete cost coverage ⇢ Chi phí chưa được ghi nhận đầy đủ
Recorded estimate ⇢ Ước tính đã ghi nhận
Coverage ⇢ Độ bao phủ
measured ⇢ đã đo
missing ⇢ thiếu
Metric definitions ⇢ Định nghĩa chỉ số
Native tools ⇢ Công cụ đã gọi
No recorded estimate ⇢ Chưa có ước tính được ghi nhận
`.trim().split('\n').map(line => line.split(' ⇢ ')))
