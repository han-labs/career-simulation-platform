# CareerSim: Frontend, Backend va Syn — speaker script

> Muc dich cua bai noi: giai thich dung nhung gi CareerSim da lam. Phan **Mo rong an toan** la de xuat tuong lai, khong phai tinh nang da chay trong MVP.

## 1. Mo dau (khoang 45 giay)

CareerSim khong phai la he thong "AI chon nghe". Day la nen tang cho sinh vien tu danh gia RIASEC, lam cac nhiem vu mo phong nghe IT, nhan ket qua cham theo quy tac xac dinh, sau do dung Syn de phan chieu bang chung va chon buoc kham pha tiep theo.

Co ba lop gia tri can tach bach:

1. **Frontend React** hien thi, nhan thao tac va quan ly trang thai giao dien.
2. **Backend Spring Boot** kiem tra quyen, thuc thi quy tac nghiep vu, cham diem va tao du lieu an toan.
3. **Syn** dien giai du lieu da duoc phe duyet; no khong cham diem va khong co quyen sua diem.

Vi tach ba lop nay, neu AI khong hoat dong thi ket qua RIASEC va ket qua mo phong van ton tai, van dung, va sinh vien van nhan duoc Standard guidance.

---

## 2. Frontend React: thanh phan nao la component?

### Cach nhan biet

Trong React, **component** la ham JavaScript co ten viet hoa, nhan `props` neu can, va tra ve JSX — giao dien khai bao nhu HTML trong JavaScript. Component thuong duoc dung bang cu phap `<TenComponent />`.

Khong duoc ket luan chi dua vao duoi `.jsx`: mot file `.jsx` co the chua component, helper, hoac ca hai. Can nhin vao ham va cach no duoc su dung.

Vi du that trong du an:

- `frontend/src/app/App.jsx`: `App`, `ApplicationLayout` va `ScrollToTop` la React component. `ScrollToTop` khong ve giao dien ma tra `null`, nhung van la component vi React render va chay `useEffect` cua no khi route thay doi.
- `frontend/src/features/guidance/pages/GuidancePage.jsx`: `GuidancePage` va `EmptyState` la component. `statusLabel` va `completedLabel` chi la helper function: chung tra ve chuoi, khong tra JSX.
- `frontend/src/features/guidance/components/SynWidget.jsx`: `SynWidget`, `SynMessage`, `SourceBadge`, `AgentArtifacts` la component. Chung nhan du lieu qua props va tra ve JSX.
- `frontend/src/features/simulations/components/SimulationCard.jsx`, `SimulationWorkspace.jsx`, `SimulationNavigation.jsx`, `SimulationResult.jsx`, `SubmissionConfirmationDialog.jsx` la component UI.
- `frontend/src/features/assessment/components/AssessmentForm.jsx` va `AssessmentResult.jsx` la component UI.

Nhung thu **khong phai component**:

- `frontend/src/features/guidance/api/guidanceApi.js`: API adapter. No goi HTTP, chuan hoa JSON va tao fallback; no khong render JSX.
- `frontend/src/shared/api/httpClient.js`: HTTP client dung chung (`apiRequest`, `ApiError`).
- `frontend/src/features/assessment/hooks/useAssessment.js` va `frontend/src/features/simulations/hooks/useSimulationAttempt.js`: custom hook. Hook dong goi state va hanh vi, nhung khong tu render giao dien.
- `data/`, `utils/`, file test va `styles/index.css`: du lieu, ham xu ly, test, CSS.
- `frontend/src/main.jsx`: diem khoi dong, dung `createRoot(...).render(...)`; no khong la mot giao dien chuc nang.

### Cau hoi khi doc mot file React

Khi mo mot file, hay hoi theo thu tu nay:

1. Ham nay co tra JSX khong? Neu co, no co the la component.
2. Ten co viet hoa va co duoc dung nhu `<TenHam />` khong? Neu co, day la component.
3. No nhan `props` gi, va du lieu nao no tu giu bang `useState`?
4. No dang render, dieu phoi trang, hay dang goi API? Neu goi API, phan do nen tach thanh hook/API adapter khi lon dan.

### Tai su dung component trong CareerSim

Tai su dung khong co nghia la copy-paste giao dien. No la tach mot phan giao dien co trach nhiem ro rang, roi truyen du lieu va hanh dong vao bang props.

- `SimulationCard({ simulation })` duoc danh cho mot simulation bat ky. Trang catalog chi map danh sach simulation thanh nhieu card; card khong tu truy van database.
- `SimulationNavigation({ currentTaskIndex, totalTasks, onPrevious, onNext })` khong biet "Backend API Triage" hay "Cybersecurity". No chi biet vi tri va callback, nen dung lai duoc cho moi simulation nhieu buoc.
- `SimulationWorkspace({ task, answers, onAnswerChange, ... })` hien thi mot task hien tai. Trang attempt nam giu state; workspace nhan state qua props.
- `EmptyState({ children })` dung lai cho nhieu khu vuc Dashboard co du lieu rong nhung van giu cau truc thong diep nhat quan.
- `SynMessage` va `AgentArtifacts` tach mot turn hoi-dap va cac result/resource/comparison card. Nhieu turn chi la `messages.map(...)`, khong phai viet lai giao dien cho tung tin nhan.

### Neu muon mo rong

Nguyen tac thuc dung cho du an nay la: **page dieu phoi, component trinh bay, hook giu logic giao dien, API adapter giao tiep HTTP**.

Vi du neu bo sung "SimulationCard co nut Save for later", khong nen de card tu `fetch` truc tiep. Ta them prop `onSave(simulation.id)`; trang cha hoac hook quyet dinh co goi API hay khong. Cach nay giu `SimulationCard` dung lai duoc trong catalog, trang goi y cua Syn, va dashboard.

Neu Syn co them loai artifact "source card", nen tao `SourceCards({ sources })` rieng va them truong co cau truc vao response DTO. Khong nen day ca chuc nang vao `SynWidget`; widget nen la noi dieu phoi conversation, khong thanh mot "god component".

Khi prop bat dau qua nhieu hoac nhieu component can chung state, co the:

1. Dua hanh vi vao custom hook, tuong tu `useSimulationAttempt`.
2. Dung React Context cho state dung chung o cap app neu thuc su can; hien tai du an chua can Context cho luong Syn.
3. Tach feature theo thu muc nhu cau truc hien tai: `pages/`, `components/`, `hooks/`, `api/`, `utils/`, `data/`.

### React ho tro gi?

- **JSX + composition:** ghep component nho thanh page lon.
- **Props:** truyen du lieu va callback mot chieu; de kiem soat component tai su dung.
- **`useState`:** khi answer, loading, draft, mode Syn thay doi, React render lai phan can thay doi.
- **`useEffect`:** chay side effect sau render, vi du tai Dashboard khi `GuidancePage` mount.
- **`useCallback` va custom hook:** giu va tai su dung hanh vi; du an dung `useAssessment` va `useSimulationAttempt`.
- **React Router:** `BrowserRouter` va `Routes` doi URL thanh page ma khong reload toan bo trang.
- **React DOM reconciliation:** React so sanh UI moi voi UI cu va cap nhat DOM toi thieu. Day la ly do ta mo ta UI theo state, khong tu thao tac tung DOM node.
- **`StrictMode`:** trong moi truong phat trien, giup phat hien side effect khong an toan som hon; no khong phai tinh nang business.

---

## 3. Tu luc mo trang den luc thay Dashboard

Hay lay route `/dashboard` lam vi du. Day la luong da co trong ma nguon.

1. Trinh duyet gui `GET /` hoac `/dashboard` den Nginx gateway.
2. Nginx chuyen request giao dien den container `frontend`. Frontend la ban build React do Vite tao ra; Vite giup dev server/build nhanh, nhung khong phai server xu ly nghiep vu trong production.
3. Trinh duyet tai HTML, JavaScript bundle va CSS. `frontend/src/main.jsx` tim `#root`, goi `createRoot`, boc app trong `StrictMode` va `BrowserRouter`.
4. `App.jsx` de React Router khop URL `/dashboard` voi `<GuidancePage />`. `ApplicationLayout` dong thoi render `AppShell` va `SynWidget` dung chung tren cac route.
5. Lan render dau, `GuidancePage` co `dashboard = null`, vi vay hien thi "Loading your dashboard…".
6. `useEffect` cua `GuidancePage` goi `getExplorationDashboard()` trong `guidanceApi.js`.
7. API adapter goi `apiRequest('/v1/guidance/dashboard')`. `httpClient.js` tao `fetch('/api/v1/guidance/dashboard')`, kem header `Accept: application/json`.
8. Nginx nhan `/api/` va proxy den Spring Boot: `backend:8080/api/...`.
9. Spring Security kiem tra request. Endpoint guidance can authenticated student. Trong demo noi bo chi khi bat `LOCAL_DEMO_MODE=true` va goi tu localhost, `LocalDemoAuthenticationFilter` moi cap demo principal; day khong phai dang nhap production.
10. `GuidanceController.getDashboard` chi la HTTP adapter. No lay `Principal`, goi `GuidanceService.getDashboard(...)`, roi boc DTO trong `ApiResponse`.
11. `GuidanceServiceImpl` kiem tra account ton tai, role `STUDENT`, status `ACTIVE`; sau do doc dung phan evidence can thiet qua `AssessmentEvidenceAccess`, `SimulationEvidenceAccess` va `GuidanceDao`.
12. PostgreSQL tra ve assessment da hoan thanh, toi da 10 simulation da duoc evaluation, va plan hien tai. Service chuyen chung thanh `DashboardResponse`: top 3 RIASEC signal, skills, recent results, progress va plan. Khong co bang `dashboard` sao chep du lieu.
13. JSON di nguoc qua controller → Nginx → `fetch`. `apiRequest` kiem tra HTTP va envelope `{ success, data }`; `normalizeDashboard` bao ve frontend truoc field thieu.
14. `setDashboard(data)` thay doi React state. React render lai `GuidancePage` voi progress, RIASEC, skills, history va plan. Ket qua objective da do backend tinh/luu khong bi frontend tu y thay doi.

Neu endpoint chua su dung duoc do sign-in chua tich hop, `guidanceApi.js` chi dung `previewDashboard` voi mot nhom loi co the fallback. UI hien nhan `Preview · Standard`; no khong gia vo la AI da phan tich du lieu that.

---

## 4. Mot hanh trinh end-to-end cu the: "Compare backend and frontend"

1. Sinh vien bam quick action trong `SynWidget`: `COMPARE_PATHS`.
2. Widget lay session ID dang co trong `localStorage`, ngon ngu `AUTO/EN/VI`, context neu co, roi goi `sendSynMessage(...)`.
3. API adapter POST JSON den `/api/v1/guidance/syn/messages`.
4. Nginx ap dung rate limit rieng cho Syn: 10 request/phut theo dia chi client, co burst nho; request body Syn toi da 16 KiB. Day la lop bao ve truoc khi vao Spring.
5. Bean Validation kiem tra message bat buoc, toi da 600 ky tu, action type va response language nam trong tap duoc cho phep.
6. `GuidanceServiceImpl` kiem tra dung sinh vien va, neu co `attemptId`, chi chap nhan attempt da evaluate thuoc sinh vien do.
7. `SynAgentOrchestrator` tai session cua dung `student_id` hoac tao session moi. Session chi luu summary co cau truc, intent cu va path cu; khong luu raw conversation.
8. `SynAgentPolicy` chuan hoa input, nhan tieng Viet/khong dau va alias nhu `BE`, `FE`, sau do quyet dinh intent va tool plan co dinh. Voi compare, plan la `READ_EVIDENCE`, `COMPARE_PATHS`, `SUGGEST_SIMULATIONS`.
9. `SynAgentToolbox` thi hanh tool plan tren du lieu do server lam chu: evidence da chon, `career_path_profiles` dang ACTIVE va `career_simulations` dang PUBLISHED. No tao comparison card, evidence references, activity summary va optional simulation cards.
10. Quick action **khong goi Gemini**. `StandardGuidancePolicy` tao loi giai thich xac dinh, vi vay hanh vi nhanh, lap lai duoc va van chay khi khong co API key.
11. Orchestrator cap nhat memory ma hoa gon nhe, han 30 ngay; sau do tra `SynMessageResponse` co `provenance: STANDARD`.
12. Widget them user message va assistant message vao state, hien comparison cards/evidence/activity. Transcript UI duoc luu co gioi han trong `sessionStorage`; no chi la tien ich hien thi cho tab hien tai, khong phai memory ma model doc.

---

## 5. Syn la loai agent gi? Co RAG hay memory khong?

Syn la **bounded, orchestrated, single-agent workflow** — mot agent bi gioi han boi policy, tool allowlist va mot vong xu ly. No khong phai autonomous agent tu lap ke hoach vo han, khong chay background, khong tu duyet web, va khong co quyen ghi du lieu objective.

Tinh nang chinh da co:

- Giai thich RIASEC va reviewed simulation result tu evidence duoc cap quyen.
- So sanh Backend, Frontend, Data, Testing, Cybersecurity.
- Chi ra noi can them evidence, khong danh gia nang luc hay "diem yeu" tuyet doi.
- Goi y simulation tiep theo va learning resource da REVIEWED.
- Tao plan draft; chi luu khi sinh vien bam xac nhan qua endpoint rieng.
- Hoi dap English/Vietnamese, ke ca mot so viet tat/khong dau.
- Follow-up co ngu canh qua memory ma hoa, co the clear.
- Hien activity, evidence references, result/resource/comparison card va nhan `AI-assisted` hoac `Standard guidance`.

**Co memory:** co, nhung la memory co gioi han. Bang `syn_sessions` luu summary versioned (language, stage, goal, covered topics, proposed next action), last intent va last paths; owner-scoped, het han sau 30 ngay, co nut xoa. No khong luu chat transcript tho va khong luu suy luan an cua model.

**Khong co RAG theo nghia ky thuat thong dung:** hien tai khong co embedding, vector database, chunking hay semantic similarity retrieval. Syn co **controlled retrieval** bang JDBC tu PostgreSQL: evidence ca nhan, reviewed `career_path_profiles`, `learning_resources`, va simulation PUBLISHED. Day la tra cuu co allowlist va filter, khong phai RAG vector. Goi no la RAG se lam sai thiet ke da lam.

---

## 6. Cau tao Syn va luong free text co Gemini

Hinh dung Syn nhu mot nhom vai tro, moi vai tro chi lam mot viec.

1. **`SynWidget` (React):** lay cau hoi, session ID, ngon ngu; hien thi ket qua. No khong tu quyet dinh nghe hay tu goi database.
2. **`GuidanceController`:** nhan HTTP va chuyen cho service. Controller khong viet prompt va khong query DAO.
3. **`GuidanceServiceImpl`:** facade cua use case. No kiem tra student, so huu attempt, tong hop Dashboard evidence, audit provenance va luu plan khi co xac nhan.
4. **`SynAgentOrchestrator`:** nhac truong. No tai memory, goi policy, goi toolbox, quyet dinh co du dieu kien goi provider hay khong, bat loi provider de fallback, cap nhat memory va dong goi response.
5. **`SynAgentPolicy`:** bo nao deterministic. No nhan intent, path alias, language, depth va safety boundary; sau do chon maximum mot tool plan tu allowlist. Model khong duoc quyen chon SQL/tool.
6. **`SynAgentToolbox`:** ban tay co kiem soat. No doc evidence duoc cap quyen, tim path/resource/simulation da duyet va tao server-owned cards. Card khong den tu van ban model nen model khong the tu phat minh link hay score.
7. **`StandardGuidancePolicy`:** cau tra loi an toan/xac dinh. Day la output mac dinh cho quick action, input nguy hiem, chua consent, provider unavailable hay provider fail.
8. **`SynAgentDao` / `JdbcSynAgentDao`:** chi truy van va luu session/data curated. Query session luon co `student_id`; resource chi `REVIEWED`, simulation chi `PUBLISHED`.
9. **`GeminiSynGuidanceProvider`:** adapter tu interface noi bo `SynGuidanceProvider` sang Gemini API. Co the thay bang `OpenAiSynGuidanceProvider` ma khong doi controller/service contract.
10. **PostgreSQL + Flyway:** Postgres la source of truth; Flyway quan ly migration. Objective score/evaluation luu tach voi guidance audit, nen AI khong the overwrite score.

Voi **free-text hop le**, luong giong quick action den het buoc toolbox. Diem khac la orchestrator chi goi provider neu dong thoi dung cac dieu kien: `actionType=FREE_TEXT`, sinh vien da consent AI, provider duoc cau hinh va available, intent khong phai safety boundary/confirm plan.

Khi goi Gemini, backend chi gui context da toi gian: student message, intent, ngon ngu, stage, do sau, toi da 6 covered topics, session summary ngan, toi da 6 tool observations, top signals va toi da 3 selected results/outcomes. Gemini **khong** nhan ket noi database, credentials, raw assessment answers, answer key hay full chat history.

Provider prompt ep buoc: khong doi score, khong chon nghe, khong tiet lo secret, khong tu nhan plan da luu, chi dung evidence duoc truyen. Gemini duoc yeu cau tra JSON schema gom `text` va `suggestions`. Adapter kiem tra finish reason, parse JSON, gioi han text 2,400 ky tu va suggestion 120 ky tu. Chi sau validation frontend moi progressively reveal response; day khong phai token streaming chua kiem tra.

Ket qua AI van dung result cards/resources/comparison/plan draft do server tao. Gemini chi giup **dien dat tu nhien** cua cau tra loi.

---

## 7. Fallback, nhieu kha nang va input vuot gioi han

### Neu co nhieu thu de xu ly, Syn chon the nao?

Syn khong "tu do suy nghi ro lai" de chon cong cu. Policy co quy tac xac dinh:

- Nếu sinh viên bấm quick action, `actionType` chỉ định intent.
- Với free text, policy nhận diện từ khóa/alias, ví dụ `so sánh`, `tài liệu`, `cải thiện`, `kế hoạch`, `BE`, `FE`.
- Nếu nhắc hai path, tool plan có `COMPARE_PATHS`; nếu hỏi resource, có `FIND_RESOURCES`; nếu hỏi gap, có `FIND_SKILL_GAPS`.
- Nếu nhắc lại "hướng thứ hai", policy đọc `lastPaths` đã lưu thay vì đoán từ toàn bộ chat.
- Nếu không có evidence cá nhân, flow chuyển sang `GET_STARTED` và dùng catalog/path đã reviewed; không bịa strength hay career fit.
- Tool plan bị giới hạn (tối đa bốn tool trong plan hiện tại), và chỉ có tối đa một provider call.

### Input vượt phép hoặc nguy hiểm

Có nhiều lớp:

1. Nginx giới hạn rate và body.
2. DTO validation giới hạn message 600 ký tự và enum action/language.
3. `StandardGuidancePolicy.requiresSafetyBoundary` nhận diện prompt injection, yêu cầu system prompt/API key/answer key, guarantee hoặc "chọn nghề thay tôi". Khi đó Syn dừng trước provider và trả lời boundary an toàn.
4. Spring Security kiểm tra authentication; service kiểm tra role, status và ownership của evidence/session.
5. Provider output phải qua schema/length parsing. Timeout, transport error, provider rejection hay JSON lỗi đều chuyển sang Standard guidance; không lộ lỗi raw của nhà cung cấp.

**Fallback không phải màn hình lỗi rỗng.** Toolbox vẫn tạo evidence card, comparison/resource/simulation/plan artifacts đã được kiểm tra; `StandardGuidancePolicy` chỉ thay phần diễn đạt. API trả provenance `STANDARD`, còn audit database ghi `FALLBACK` hoặc `REPLACED_BY_FALLBACK` và model name khi thành công AI.

---

## 8. Model: cần hiệu chỉnh tên trước khi thuyết trình

Trong cấu hình hiện tại, Gemini mặc định là `gemini-3.5-flash-lite` trong `docker-compose.yml` và `application.yml`.

Theo tài liệu Gemini chính thức tại thời điểm kiểm tra, không có model ID `gemini-3.6-flash-lite`. Tên chính xác gần nhất là **`gemini-3.6-flash`**; còn nhánh Lite là **`gemini-3.5-flash-lite`**. Vì vậy, trong báo cáo không nên ghi “3.6 Flash-Lite” như một model ID thật.

Nếu mục tiêu nâng cấp là tăng chất lượng diễn giải, xử lý tiếng Việt và độ tin cậy cho agent có memory/tool observation, lý do chọn **Gemini 3.6 Flash (`gemini-3.6-flash`)** là:

- Đây là model stable, có structured output, phù hợp với schema JSON mà adapter hiện dùng.
- Google mô tả 3.6 Flash tối ưu cân bằng tốc độ, chi phí và năng lực agentic/general tasks; phù hợp với một lần synthesis ngắn của Syn.
- Syn đã tự làm routing, retrieval và kiểm soát bằng code. Vì vậy không cần model Pro nặng/đắt để tự lập kế hoạch dài; chỉ cần model có chất lượng diễn đạt và tuân thủ schema tốt.
- Provider được bọc bằng adapter nên thay model chủ yếu là thay biến `GEMINI_MODEL`; HTTP contract, evidence boundary và fallback giữ nguyên. Tuy nhiên vẫn phải chạy lại test structured output, timeout, Vietnamese, prompt injection và smoke test trước khi công bố.

Nếu ưu tiên chính là chi phí cực thấp, lượt gọi nhiều và câu trả lời đơn giản, giữ `gemini-3.5-flash-lite` hợp lý hơn. Không nên gọi 3.6 Flash là Lite, vì đó là hai trade-off khác nhau.

Nguồn chính thức: [Gemini 3.6 Flash](https://ai.google.dev/gemini-api/docs/models/gemini-3.6-flash), [danh sách model Gemini](https://ai.google.dev/gemini-api/docs/models), [release notes tháng 07/2026](https://ai.google.dev/gemini-api/docs/changelog).

---

## 9. Mo rong an toan: them search engine tool (CHUA co trong MVP)

Hien tai Syn **khong** co web search tool. Ke hoach US-03 va implementation loai tru unrestricted web browsing. Neu noi "Syn dang search Google" thi se sai voi du an hien tai.

Neu sau nay duoc duyet scope de them search engine, thiet ke dung khong phai la dua URL cho model va bao no "doc giup". Can them mot `SearchAdapter` server-side va mot tool `SEARCH_REVIEWED_SOURCES` trong allowlist. Luong an toan nen la:

1. **Policy gate:** chi mot so intent, vi du `LEARNING_RESOURCES`, moi duoc de xuat search. User khong the bat model goi search bang prompt injection.
2. **Query builder:** server tao query tu skill/path da duoc resolve, khong dung nguyen van moi huong dan trong user input. Co allowlist domain (vi du official docs, university/verified course provider), time/cost limit va safe-search policy.
3. **SearchAdapter:** goi API search bang server credential. Model khong co API key va khong goi URL truc tiep.
4. **Source filter:** kiem tra domain allowlist, HTTPS, duplicate, language, license/provenance va document type. Trang khong dat yeu cau bi loai.
5. **Evidence extractor + verifier:** tach title, publisher, URL, date; doi chieu voi skill/path ma policy da chon. Khong cho phep web page chen "ignore instructions" thanh lenh.
6. **Ranking co giai thich:** rank theo do phu hop skill truoc, sau do path, do tin cay publisher va freshness. Luu source/provenance, review status; hien link/card do server tao.
7. **Reasoning grounded:** model chi nhan top ket qua da loc va evidence ca nhan. No co the tom tat, so sanh, neu gioi han bang chung va de xuat mot next step; khong duoc bien source thanh ket luan nang luc/nghe nghiep.
8. **Human/content review:** voi resource muon dua vao catalog lau dai, luu record `PENDING_REVIEW`; chi `REVIEWED` moi duoc Syn goi y cho sinh vien. Search result tuc thoi cung phai hien source va disclaimer.
9. **Fallback:** search API fail/het quota/khong co result thi tro lai resource da REVIEWED trong PostgreSQL hoac noi ro "chua tim thay nguon da kiem tra", khong hallucinate link.

Voi thiet ke nay, agent lam duoc nhieu hon "tra cuu tai lieu": no doi chieu cau hoi voi evidence cua chinh sinh vien, kiem tra ownership/an toan, chon nguon co provenance, kiem tra logic phu hop skill-path, tao artifact co cau truc, va chi sau do moi nho model dien dat. Day la cach giu **control, reasoning va evidence** o backend thay vi giao ca quyet dinh cho LLM.

---

## 10. Ket (khoang 30 giay)

CareerSim dung React de chia giao dien thanh component, hook va API adapter; dung Spring Boot de giu quy tac nghiep vu, phan quyen va orchestration; dung PostgreSQL/Flyway de luu evidence ben vung; dung Syn nhu mot agent bi gioi han de dien giai evidence. Gia tri cua Syn khong nam o viec no noi nghe hay, ma nam o viec no chi duoc noi tren bang chung da duoc he thong kiem tra, co provenance va luon co fallback an toan.
