// System labels only. Never translate authored names, instructions, conversations or quotations.
export const workspaceMessages = Object.fromEntries(`
Agent is preparing a response ⇢ Trợ lý đang chuẩn bị phản hồi
Knowledge Bases ⇢ Kho tri thức
Knowledge Base ⇢ Kho tri thức
AI Agents ⇢ Trợ lý AI
Brain ⇢ Cấu hình chính
English (EN) ⇢ Tiếng Anh (EN)
Vietnamese (VI) ⇢ Tiếng Việt (VI)
ACTIVE ⇢ Hoạt động
DRAFT ⇢ Bản nháp
PUBLISHED ⇢ Đã công bố
ARCHIVED ⇢ Đã lưu trữ
HIDDEN ⇢ Đã ẩn
READY ⇢ Sẵn sàng
SUCCEEDED ⇢ Thành công
FAILED ⇢ Thất bại
REFUSED ⇢ Đã từ chối
CANCELLED ⇢ Đã hủy
RUNNING ⇢ Đang chạy
ACCEPTED ⇢ Đã tiếp nhận
NOT_APPLICABLE ⇢ Không áp dụng
UNKNOWN ⇢ Chưa xác định
KB_TEST ⇢ Kiểm thử kho tri thức
ADMIN_PREVIEW ⇢ Xem trước quản trị
STUDENT_CHAT ⇢ Trò chuyện sinh viên
STUDENT ⇢ Sinh viên
ADMIN ⇢ Quản trị viên
ADMINISTRATOR ⇢ Quản trị viên
ENTERPRISE ⇢ Doanh nghiệp
GENERAL_ALLOWED ⇢ Cho phép hỗ trợ tổng quát
KB_ONLY ⇢ Chỉ dựa trên kho tri thức
MODEL_ATTEMPT ⇢ Lần gọi mô hình
ACTUAL_NATIVE_CALL ⇢ Gọi công cụ
OUTPUT_VALIDATION ⇢ Kiểm tra đầu ra
OUTPUT_REPAIR ⇢ Sửa định dạng đầu ra
EVIDENCE_RECONSIDERATION ⇢ Xem xét lại bằng chứng
PRIMARY ⇢ Mô hình chính
CHAT ⇢ Trò chuyện
INSIGHT_REPORT ⇢ Báo cáo tổng hợp
PLAN_SUGGESTION ⇢ Gợi ý kế hoạch nháp
NOT_RECORDED ⇢ Chưa ghi nhận
ESTIMATED ⇢ Ước tính
Sign in is required. ⇢ Vui lòng đăng nhập.
The supplied value is invalid. ⇢ Giá trị được cung cấp không hợp lệ.
If-Match revision is required. ⇢ Cần gửi phiên bản đối chiếu If-Match.
Final submissions cannot be changed. ⇢ Không thể thay đổi bài đã nộp chính thức.
The request conflicts with the current state. ⇢ Yêu cầu xung đột với trạng thái hiện tại.
Please try again later. ⇢ Vui lòng thử lại sau.
The service is temporarily unavailable. ⇢ Dịch vụ tạm thời không khả dụng.
The request is invalid. ⇢ Yêu cầu không hợp lệ.
You are not allowed to perform this action. ⇢ Bạn không có quyền thực hiện thao tác này.
The requested resource was not found. ⇢ Không tìm thấy tài nguyên được yêu cầu.
One or more fields are invalid. ⇢ Một hoặc nhiều trường không hợp lệ.
The request could not be completed. Please try again later. ⇢ Chưa thể hoàn tất yêu cầu. Vui lòng thử lại sau.
Career Simulations ⇢ Mô phỏng
Career Quizzes ⇢ Trắc nghiệm
For Enterprise ⇢ Doanh nghiệp
How It Works ⇢ Cách sử dụng
Sign In / Sign Up ⇢ Đăng nhập / Đăng ký
AI-Assisted Research ⇢ Nghiên cứu với sự hỗ trợ của AI
AI Collaboration ⇢ Cộng tác với AI
AI-Guided Problem Solving ⇢ Giải quyết vấn đề với AI
Analytical Skills ⇢ Kỹ năng phân tích
Analytical Thinking ⇢ Tư duy phân tích
API Development ⇢ Phát triển API
Attention to Detail ⇢ Chú ý đến chi tiết
Basic HTML Troubleshooting ⇢ Khắc phục lỗi HTML cơ bản
Business Communication ⇢ Giao tiếp trong công việc
Command Line ⇢ Dòng lệnh
Communication ⇢ Giao tiếp
Critical Thinking ⇢ Tư duy phản biện
Data Visualization ⇢ Trực quan hóa dữ liệu
Development Tools ⇢ Công cụ phát triển
Documentation ⇢ Tài liệu
Effective Communication ⇢ Giao tiếp hiệu quả
Information Security ⇢ An toàn thông tin
Information Synthesis ⇢ Tổng hợp thông tin
Infrastructure Management ⇢ Quản lý hạ tầng
Initiative and Self-Led Learning ⇢ Chủ động và tự học
IT Service Management ⇢ Quản lý dịch vụ CNTT
Java Programming ⇢ Lập trình Java
Open Source Intelligence ⇢ Thu thập thông tin từ nguồn mở
Options Analysis ⇢ Phân tích các phương án
Presentation Design ⇢ Thiết kế bài trình bày
Presentation Skills ⇢ Kỹ năng thuyết trình
Problem Solving ⇢ Giải quyết vấn đề
Process Improvement ⇢ Cải tiến quy trình
Prompt Engineering ⇢ Kỹ thuật viết yêu cầu cho AI
Proposal Development ⇢ Xây dựng đề xuất
Report Writing ⇢ Viết báo cáo
Requirements Analysis ⇢ Phân tích yêu cầu
Research ⇢ Nghiên cứu
REST API Development ⇢ Phát triển REST API
Risk Assessment ⇢ Đánh giá rủi ro
Risk Management ⇢ Quản lý rủi ro
Root Cause Analysis ⇢ Phân tích nguyên nhân gốc rễ
Sales Management ⇢ Quản lý kinh doanh
Sales Process ⇢ Quy trình kinh doanh
Security Analysis ⇢ Phân tích an ninh
Service Delivery ⇢ Cung cấp dịch vụ
Software Evaluation ⇢ Đánh giá phần mềm
Stakeholder Communication ⇢ Giao tiếp với các bên liên quan
Technical Communication ⇢ Giao tiếp kỹ thuật
Test Frameworks ⇢ Công cụ kiểm thử
Time Management ⇢ Quản lý thời gian
Unit Testing ⇢ Kiểm thử đơn vị
Version Control ⇢ Quản lý phiên bản
Visual Storytelling ⇢ Kể chuyện bằng hình ảnh
Written Communication ⇢ Giao tiếp bằng văn bản
Submit a zip file containing your Java code here ⇢ Nộp tệp ZIP chứa mã Java tại đây
Submit your Excel worksheet here ⇢ Nộp bảng tính Excel tại đây
Submit your PowerPoint presentation here ⇢ Nộp bài trình bày PowerPoint tại đây
Submit your proposal here ⇢ Nộp bản đề xuất tại đây
Submit your Solution Options Spreadsheet here ⇢ Nộp bảng phân tích phương án giải pháp tại đây
Task response ⇢ Bài làm
Type your answer below ⇢ Nhập câu trả lời bên dưới
Upload a Python file containing the unit tests here ⇢ Tải lên tệp Python chứa các kiểm thử đơn vị tại đây
Upload a zip file containing your Java code here ⇢ Tải lên tệp ZIP chứa mã Java tại đây
Upload your completed Risk Assessment template here ⇢ Tải lên biểu mẫu đánh giá rủi ro đã hoàn thành tại đây
Upload your Findings and Recommendations Document here ⇢ Tải lên tài liệu kết quả và khuyến nghị tại đây
Upload your process flow chart here ⇢ Tải lên sơ đồ quy trình tại đây
Upload your repository link here ⇢ Nhập link kho mã tại đây
Upload your task status email here ⇢ Tải lên email cập nhật tình trạng công việc tại đây
Upload your ZIP folder of the code base here ⇢ Tải lên tệp ZIP chứa mã nguồn tại đây
Q 1/11: Datacom solution areas include: ⇢ Câu 1/11: Các lĩnh vực giải pháp của Datacom gồm:
Learn about Java Spring Boot ⇢ Tìm hiểu Java Spring Boot
HPE Greenlake Video ⇢ Video giới thiệu HPE GreenLake
HPE GreenLake Website ⇢ Trang web HPE GreenLake
How to Create a REST API using Java Spring Boot ⇢ Cách tạo REST API bằng Java Spring Boot
Gradle Install ⇢ Cài đặt Gradle
Building a RESTful Web Service ⇢ Xây dựng dịch vụ web RESTful
GitHub for gs-rest-service ⇢ Kho mã GitHub gs-rest-service
Creating a RESTful HTTP Server in Spring Boot (Java) - With Full Examples ⇢ Tạo máy chủ HTTP RESTful bằng Spring Boot (Java), kèm ví dụ
Unit Testing in Spring Boot Project Using Mockito and JUnit ⇢ Kiểm thử đơn vị cho Spring Boot bằng Mockito và JUnit
Guide to Unit Testing Spring Boot REST APIs ⇢ Hướng dẫn kiểm thử đơn vị cho REST API Spring Boot
The basic principles of customer service excellence ⇢ Các nguyên tắc chăm sóc khách hàng hiệu quả
Incident Report Template ⇢ Biểu mẫu báo cáo sự cố
Step-by-step guidance to create a GitHub account ⇢ Hướng dẫn tạo tài khoản GitHub
Tutorial: Get started with Visual Studio Code ⇢ Hướng dẫn bắt đầu với Visual Studio Code
Step-by-step guidance to create an Azure free account ⇢ Hướng dẫn tạo tài khoản Azure miễn phí
Site to create an Azure free account ⇢ Trang tạo tài khoản Azure miễn phí
Git Configuration Instructions ⇢ Hướng dẫn cấu hình Git
Step-by-step guidance to create a new repository ⇢ Hướng dẫn tạo kho mã mới
Terminal user guide (MacOS) ⇢ Hướng dẫn sử dụng terminal trên macOS
Terminal user guide (Windows) ⇢ Hướng dẫn sử dụng terminal trên Windows
Install Git (MacOS) ⇢ Cài đặt Git trên macOS
Install Git (Windows) ⇢ Cài đặt Git trên Windows
Step-by-step guidance to create a repository ⇢ Hướng dẫn sao chép kho mã về máy
Step-by-step guidance to create a variable in GitHub ⇢ Hướng dẫn tạo biến trên GitHub
Step-by-step guidance to create a GitHub Action Workflow ⇢ Hướng dẫn tạo quy trình GitHub Actions
Customer Questions Worksheet ⇢ Bảng câu hỏi dành cho khách hàng
Customer Questions with Answers ⇢ Câu hỏi và phản hồi của khách hàng
Solutions Options Spreadsheet ⇢ Bảng phân tích các phương án giải pháp
PowerPoint Template with Instructions ⇢ Mẫu PowerPoint kèm hướng dẫn
HPE Nimble Storage dHCI QuickSpecs ⇢ Thông số kỹ thuật HPE Nimble Storage dHCI
HPE - Hyperconverged Infrastructure ⇢ HPE - Hạ tầng siêu hội tụ
HPE Presentation Template ⇢ Mẫu bài trình bày HPE
Welcome to HPE Sales Professional Document ⇢ Tài liệu nhập môn kinh doanh HPE
HPE Website ⇢ Trang web HPE
How to Give a Killer Presentation ⇢ Cách thuyết trình thuyết phục
HPE Customer Research Template ⇢ Biểu mẫu nghiên cứu khách hàng HPE
Customers ⇢ Khách hàng
Error Log ⇢ Nhật ký lỗi
Process Data ⇢ Xử lý dữ liệu
Transactions ⇢ Giao dịch
Task 2 Instructions ⇢ Hướng dẫn nhiệm vụ 2
Task 3 Instruction ⇢ Hướng dẫn nhiệm vụ 3
Threat and Vulnerability Identification Template ⇢ Biểu mẫu nhận diện mối đe dọa và lỗ hổng
Customer Feedback ⇢ Phản hồi khách hàng
Link to Sandbox Environment ⇢ Mở môi trường thực hành
What are browser developer tools? ⇢ Công cụ dành cho nhà phát triển trên trình duyệt là gì?
Learn React ⇢ Học React
JavaScript Tutorial ⇢ Hướng dẫn JavaScript
Where X=javascript ⇢ Tìm hiểu JavaScript qua ví dụ
`.trim().split('\n').map(line => line.split(' ⇢ ')))
