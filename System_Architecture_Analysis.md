# Phân tích luồng vận hành toàn hệ thống (Career Simulation Platform)

Tài liệu này phân tích chi tiết luồng vận hành của hệ thống từ lúc khởi động, từ Frontend (React/Vite) xuống Backend (Spring Boot), kèm theo các đoạn mã minh họa cho từng bước thực thi.

---

## 1. Hệ thống khởi động (Khi vừa chạy lên)

### 1.1. Backend (Spring Boot)
- **Điểm bắt đầu**: File `CareerSimulationApplication.java` được chạy. Framework Spring Boot bắt đầu quét (scan) toàn bộ mã nguồn, tìm kiếm các Annotation như `@RestController`, `@Service`, `@Repository`.
- **Controllers khởi tạo**: Các Controller sẽ đăng ký các đường dẫn (Endpoint) API. Ví dụ:
  - `SimulationCatalogController.java` đăng ký đường dẫn `/v1/simulations`.
  - `AssessmentController.java` đăng ký đường dẫn `/v1/assessments`.
- Ứng dụng Backend chờ nhận Request từ trình duyệt.

### 1.2. Frontend (React/Vite)
- Khi truy cập trình duyệt (VD: `http://localhost:5173`), Vite sẽ tải file `index.html`, từ đó nạp file `main.jsx`.
- **Tại `main.jsx`**: Hệ thống sẽ wrap toàn bộ ứng dụng bằng `<BrowserRouter>` (Quản lý các URL/Route) và gọi `<App />`.

```jsx
// frontend/src/main.jsx
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
```

- **Tại `App.jsx`**: Định tuyến (Router) kiểm tra URL hiện tại của trình duyệt để quyết định load Component nào.
  - Nếu URL là `/` (trang chủ) ➔ Load `<LandingPage />`.
  - Nếu URL là `/assessment` ➔ Load `<AssessmentPage />`.

```jsx
// frontend/src/app/App.jsx
function App() {
  return (
    <Routes>
      <Route element={<ApplicationLayout />}>
        <Route index element={<LandingPage />} />
        <Route path="assessment" element={<AssessmentPage />} />
        {/* Các trang khác... */}
      </Route>
    </Routes>
  )
}
```

---

## 2. Luồng chạy cụ thể của từng trang

### 2.1. Trang chủ (Landing Page)
Khi người dùng truy cập `URL: /`:

**Bước 1: Trình duyệt load `<LandingPage />`**
- React chạy hàm `useEffect` đầu tiên ngay sau khi component mount lên giao diện.
- Nó gọi hàm `getPublishedSimulations()` (trong API của Frontend).

```jsx
// frontend/src/features/landing/pages/LandingPage.jsx
useEffect(() => {
  getPublishedSimulations()
    .then((data) => {
      setSimulations(data.slice(0, 3)) // Chỉ lấy 3 bài demo
      setCatalogStatus('ready')
    })
}, [])
```

**Bước 2: Gửi Request xuống Backend**
- API Frontend gọi Request GET tới Backend: `GET /v1/simulations`.
- Backend (`SimulationCatalogController.java`) nhận request này:

```java
// backend/src/main/java/edu/hcmute/careersim/catalog/controller/SimulationCatalogController.java
@GetMapping
public ApiResponse<List<SimulationSummaryResponse>> getPublishedSimulations() {
    return ApiResponse.success(simulationCatalogService.getPublishedSimulations());
}
```

- Logic Backend truy vấn Database để lấy các bài Simulation đã Publish và trả về chuỗi JSON.

**Bước 3: Frontend nhận Data và Render giao diện**
- Data trả về từ Backend sẽ cập nhật vào State `simulations`. React phát hiện thay đổi state ➔ render lại giao diện.
- `LandingPage` truyền data này vào các Component con:
  - `<HeroSection />`: Render màn giới thiệu banner.
  - `<JourneySection />`: Render 3 bước quy trình.
  - `<CatalogSection simulations={simulations} />`: Chạy vòng lặp (Map) để in ra `<SimulationCard />`.

---

### 2.2. Trang Đánh giá năng lực (Assessment Page)
Khi người dùng bấm "Start with RIASEC" ➔ URL chuyển sang `/assessment`.

**Bước 1: Load `<AssessmentPage />` và lấy câu hỏi**
- `AssessmentPage` mount lên, gọi Hook tùy chỉnh `useAssessment()`. Hook này kích hoạt `fetchQuestions()`.
- Frontend gửi API: `GET /v1/assessments/questions`.
- Backend (`AssessmentController.java`) xử lý và trả về bộ câu hỏi RIASEC:

```java
// backend/src/main/java/edu/hcmute/careersim/assessment/controller/AssessmentController.java
@GetMapping("/questions")
public ApiResponse<List<RiasecQuestionDto>> getQuestions() {
    return ApiResponse.success(assessmentService.getActiveQuestions());
}
```

**Bước 2: Hiển thị Màn Giới Thiệu (Intro)**
- Dữ liệu trả về được lưu vào state, lúc này `status` đang là `intro`.
- Giao diện render Component `<AssessmentIntro />`. Người dùng đọc luật chơi.

**Bước 3: Bắt đầu làm bài**
- Người dùng nhấn nút "Start Assessment". Trình duyệt gọi hàm `startAttempt()`:
  - Frontend gọi API `POST /v1/assessments/attempts`. Backend khởi tạo phiên làm bài (Session) và lưu Database.
  - State của Frontend `status` chuyển thành `in_progress`.
- `<AssessmentPage />` render ra form làm bài với danh sách các `<AssessmentQuestionCard />` và thanh `<AssessmentProgressBar />`.

```jsx
// Trích xuất từ AssessmentPage.jsx
{status === 'in_progress' && isTakingTest && (
  <AssessmentForm 
    questions={questions}
    answers={answers}
    // ...
  />
)}
```

**Bước 4: Submit bài làm**
- Sau khi click xong các đáp án, người dùng bấm "Submit".
- Hàm `submitAnswers()` trong Frontend gọi API `POST /v1/assessments/attempts/{id}/submit` kèm theo body chứa mọi ID câu hỏi và Value (1-5).
- Backend (`AssessmentController.java`) nhận payload, chạy logic thuật toán (cộng điểm của 6 hệ RIASEC: Realistic, Investigative, Artistic, Social, Enterprising, Conventional) và lưu vào Database.
- Backend trả về kết quả `AssessmentResultDto` (chứa Profile tính cách nổi bật nhất).

**Bước 5: Render Kết Quả**
- Frontend nhận kết quả, state `status` được đổi thành `completed`.
- `<AssessmentPage />` ẩn màn hình Form và render ra Component `<AssessmentResult />` để người dùng xem điểm số và lời khuyên định hướng.

---

## 3. Tổng kết kiến trúc

Toàn bộ luồng đi này hoạt động theo nguyên tắc **Client-Server Separation (Tách biệt UI và Xử lý)**:
1. **Trình duyệt (Client)**: Luôn đi đầu trong việc xác định Route, render Component dạng vỏ bọc (Skeleton) và gọi API Fetch dữ liệu.
2. **Backend (Server)**: Là nơi chứa tính toàn vẹn (Single Source of Truth), quản lý nghiệp vụ như tính toán điểm số (Assessment), truy vấn Data, và trả về qua REST API JSON.
3. **Cơ chế React Hooks**: Nhờ `useEffect` (side-effects lúc khởi tạo) và `useState` (quản lý thay đổi cục bộ), UI luôn được thay đổi uyển chuyển mà không bị tải lại (Load) trắng trang.
