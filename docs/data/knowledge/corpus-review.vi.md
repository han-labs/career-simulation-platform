# Corpus Syn — bản để người dùng đọc và review

Đây là bản nháp, chưa publish. Xác nhận kế hoạch không tự thay thế review nội dung. Sau khi đọc, sửa nội dung nếu cần, tạo corpus.approved.json với reviewer thật và reviewed=true cho từng tài liệu được duyệt. Giữ sourceKey để versioning.

## frontend-accessibility-en

**Accessible form labels** — EN · FRONTEND_DEVELOPMENT

A form control needs a label that describes what to enter. Associate the label with the control so assistive technology can announce it. Placeholder text is a hint, not a replacement for a persistent label. An error should identify the field and explain how to correct the input. Test the form with a keyboard as well as a pointer. In a CareerSim exercise, inspecting labels and keyboard focus provides task evidence; it does not establish a final career fit.

## frontend-accessibility-vi

**Nhãn biểu mẫu dễ tiếp cận** — VI · FRONTEND_DEVELOPMENT

Một ô nhập liệu cần nhãn mô tả nội dung phải nhập. Liên kết nhãn với ô để công nghệ hỗ trợ đọc được tên của nó. Placeholder là gợi ý, không thay thế nhãn luôn hiển thị. Thông báo lỗi cần chỉ rõ ô gặp lỗi và cách sửa. Kiểm tra biểu mẫu bằng bàn phím bên cạnh chuột. Trong bài tập CareerSim, việc kiểm tra nhãn và vị trí focus cung cấp bằng chứng thực hành, không kết luận mức độ phù hợp nghề nghiệp.

## frontend-state-en

**Loading and failure states** — EN · FRONTEND_DEVELOPMENT

An interface that fetches data should distinguish loading, success, empty results and failure. An empty result is a successful response containing no records; failure means the request could not be completed. Preserve user input during a recoverable failure and offer an explicit retry. Disable duplicate submissions while a request is pending. Display objective results separately from optional generated explanations.

## frontend-state-vi

**Trạng thái tải và lỗi** — VI · FRONTEND_DEVELOPMENT

Giao diện tải dữ liệu cần phân biệt đang tải, thành công, kết quả rỗng và thất bại. Kết quả rỗng là phản hồi thành công nhưng không có bản ghi; thất bại là yêu cầu chưa hoàn tất. Giữ nội dung người dùng đã nhập khi có lỗi có thể thử lại và cung cấp nút thử lại rõ ràng. Chặn gửi trùng khi yêu cầu đang chờ. Hiển thị kết quả khách quan riêng với phần giải thích do AI tạo.

## backend-idempotency-en

**Idempotent requests** — EN · BACKEND_DEVELOPMENT

A network timeout does not prove that a server rejected a write. The server may have committed before the client lost the response. A client-generated request identifier can let the server recognize a retry. Store the identifier with its owner and a hash of the request payload. Replaying an identical completed request returns the recorded result. Reusing the identifier for different content should be rejected. A database uniqueness constraint supports this guarantee under concurrent requests.

## backend-idempotency-vi

**Yêu cầu có tính idempotent** — VI · BACKEND_DEVELOPMENT

Timeout mạng không chứng minh máy chủ đã từ chối ghi dữ liệu. Máy chủ có thể đã commit trước khi máy khách mất phản hồi. Mã yêu cầu do máy khách tạo giúp máy chủ nhận ra lần gửi lại. Lưu mã cùng chủ sở hữu và hash nội dung yêu cầu. Gửi lại yêu cầu hoàn thành có cùng nội dung trả về kết quả đã lưu. Tái dùng mã cho nội dung khác cần bị từ chối. Ràng buộc duy nhất trong cơ sở dữ liệu hỗ trợ bảo đảm này khi có yêu cầu đồng thời.

## backend-transactions-en

**Short database transactions** — EN · BACKEND_DEVELOPMENT

A transaction groups database changes that must succeed or fail together. Holding a transaction open while waiting for an external service can occupy a connection and prolong locks. One design reads the required evidence in a short transaction, calls the external service after that transaction ends, and commits the accepted result in another short transaction. The final step must recheck ownership, state and version because data may have changed during the external call.

## backend-transactions-vi

**Transaction cơ sở dữ liệu ngắn** — VI · BACKEND_DEVELOPMENT

Transaction gom các thay đổi cơ sở dữ liệu cần cùng thành công hoặc cùng thất bại. Giữ transaction mở trong lúc chờ dịch vụ bên ngoài có thể chiếm connection và kéo dài khóa. Một thiết kế đọc bằng chứng trong transaction ngắn, gọi dịch vụ sau khi transaction kết thúc, rồi lưu kết quả được chấp nhận trong transaction ngắn khác. Bước cuối phải kiểm tra lại chủ sở hữu, trạng thái và phiên bản vì dữ liệu có thể thay đổi trong thời gian gọi dịch vụ.

## data-joins-en

**Understanding SQL joins** — EN · DATA_ANALYSIS

A join combines rows using a relationship between tables. An inner join keeps matching rows. A left join preserves rows from the left table and supplies null values for missing matches on the right. Multiple matches can increase the row count, which can distort an aggregate if the relationship is misunderstood. Before summing joined data, inspect keys, expected cardinality and duplicate matches. Practice with synthetic records rather than private student exports.

## data-joins-vi

**Hiểu phép nối SQL** — VI · DATA_ANALYSIS

JOIN kết hợp các dòng theo quan hệ giữa các bảng. INNER JOIN giữ các dòng có kết quả khớp. LEFT JOIN giữ dòng của bảng bên trái và dùng NULL khi không có bản ghi khớp bên phải. Nhiều kết quả khớp có thể tăng số dòng, làm sai phép tổng hợp nếu hiểu sai quan hệ. Trước khi tính tổng trên dữ liệu đã nối, kiểm tra khóa, số lượng bản ghi dự kiến và kết quả khớp trùng. Thực hành với bản ghi giả lập thay vì dữ liệu sinh viên riêng tư.

## data-missing-en

**Missing values and data quality** — EN · DATA_ANALYSIS

A missing value is not automatically zero. It may mean not collected, unavailable or not applicable. Investigate the meaning before choosing a treatment. Count missing values and compare their distribution across relevant groups. Document exclusions or imputation rules so another analyst can reproduce the result. A chart should identify its denominator and data limitations. An observed association alone does not establish a causal explanation.

## data-missing-vi

**Giá trị thiếu và chất lượng dữ liệu** — VI · DATA_ANALYSIS

Giá trị thiếu không tự động bằng không. Nó có thể mang nghĩa chưa thu thập, không có sẵn hoặc không áp dụng. Tìm hiểu ý nghĩa trước khi chọn cách xử lý. Đếm giá trị thiếu và so sánh phân bố giữa các nhóm liên quan. Ghi lại quy tắc loại bỏ hoặc điền giá trị để người khác tái lập kết quả. Biểu đồ cần nêu mẫu số và hạn chế của dữ liệu. Mối liên hệ quan sát được chưa đủ chứng minh quan hệ nhân quả.

## testing-boundaries-en

**Boundary value tests** — EN · SOFTWARE_TESTING

Boundary tests exercise the edges of an allowed range. If an input accepts values from 1 through 100, useful cases include 1, 100, 0 and 101, as well as a representative interior value. Also check missing values and incorrect types when the interface accepts structured input. Derive expected outcomes from the requirement before running the implementation. A passing example does not demonstrate that every invalid case is handled.

## testing-boundaries-vi

**Kiểm thử giá trị biên** — VI · SOFTWARE_TESTING

Kiểm thử biên kiểm tra các ranh giới của miền giá trị hợp lệ. Nếu đầu vào chấp nhận từ 1 đến 100, các trường hợp hữu ích gồm 1, 100, 0, 101 và một giá trị ở giữa. Kiểm tra cả giá trị bị thiếu và sai kiểu khi giao diện nhận dữ liệu có cấu trúc. Xác định kết quả mong đợi từ yêu cầu trước khi chạy phần triển khai. Một ví dụ thành công không chứng minh mọi đầu vào sai đều được xử lý.

## testing-regression-en

**Regression and integration tests** — EN · SOFTWARE_TESTING

A regression test protects behavior that should remain stable after a change. A unit test isolates a small rule; an integration test checks collaborators such as application code and a database together. Use a real PostgreSQL instance for PostgreSQL-specific operators and transaction behavior. Mocking a vector-search result can test orchestration but cannot verify the database distance operator. Record skipped tests separately from passed tests.

## testing-regression-vi

**Kiểm thử hồi quy và tích hợp** — VI · SOFTWARE_TESTING

Kiểm thử hồi quy bảo vệ hành vi cần giữ ổn định sau thay đổi. Unit test tách một quy tắc nhỏ; integration test kiểm tra các thành phần phối hợp, chẳng hạn ứng dụng và cơ sở dữ liệu. Dùng PostgreSQL thật để kiểm tra toán tử và transaction đặc thù PostgreSQL. Mock kết quả tìm vector kiểm tra được điều phối nhưng không xác minh toán tử khoảng cách trong cơ sở dữ liệu. Ghi nhận test bỏ qua riêng với test đã đạt.

## security-ownership-en

**Authorization and ownership** — EN · CYBERSECURITY

Authentication establishes an identity. Authorization decides whether that identity may perform a particular action on a resource. Knowing a record identifier is not permission to access it. For a private student result, check the authenticated owner on the server for each request. Hiding a button in the browser is not an authorization control. Tests should include another student, an inactive account and an inappropriate role.

## security-ownership-vi

**Phân quyền và quyền sở hữu** — VI · CYBERSECURITY

Xác thực xác định danh tính. Phân quyền quyết định danh tính đó được thực hiện hành động nào trên tài nguyên. Biết mã bản ghi không đồng nghĩa có quyền đọc. Với kết quả riêng tư của sinh viên, kiểm tra chủ sở hữu được xác thực ở máy chủ cho từng yêu cầu. Ẩn nút trên trình duyệt không phải cơ chế phân quyền. Kiểm thử cần gồm sinh viên khác, tài khoản bị khóa và vai trò không phù hợp.

## security-injection-en

**Treat retrieved text as untrusted data** — EN · CYBERSECURITY

Retrieved documents can contain instructions that conflict with application policy. Treat document text as evidence to interpret, not as authority to change tool permissions or reveal secrets. The server should own the tool allowlist and validate arguments independently of the model. Keep objective scoring outside generation. Citation identifiers must refer to sources actually retrieved for the current request. These controls reduce specific risks; they do not prove that every generated statement is true.

## security-injection-vi

**Coi văn bản truy xuất là dữ liệu chưa tin cậy** — VI · CYBERSECURITY

Tài liệu truy xuất có thể chứa chỉ dẫn trái chính sách ứng dụng. Coi văn bản là bằng chứng để diễn giải, không phải quyền thay đổi công cụ hay tiết lộ bí mật. Máy chủ quản lý danh sách công cụ và kiểm tra tham số độc lập với mô hình. Giữ chấm điểm khách quan bên ngoài bước sinh câu trả lời. Mã trích dẫn phải trỏ đến nguồn thực sự được truy xuất cho yêu cầu hiện tại. Các kiểm soát này giảm rủi ro cụ thể, không chứng minh mọi câu do AI tạo đều đúng.

