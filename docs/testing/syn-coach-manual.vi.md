# Test Syn A/B/C bằng giao diện

## Test lại sau sửa hội thoại (hoàn tất tiếp ngày 2026-10-02)

Reload trang để nạp frontend mới. Mở hội thoại mới, gõ trực tiếp vào ô chat (không bấm quick action) cho các ca sau. Free text có consent ưu tiên LLM; quick action vẫn Standard. Không cần đổi model hay xóa database.

| Câu nhập | Kết quả mong đợi |
|---|---|
| `Tôi hứng thú với backend, gợi ý nguồn học, simulation và kế hoạch trải nghiệm 2 tuần.` | Giải thích tự nhiên nêu nguồn, mục đích học, hoạt động từng tuần và đầu ra; có link/draft. Nếu output lần đầu không hợp lệ, thử sửa bằng LLM một lần trong deadline. |
| `so sánh 2 hướng frontend và backend cho tôi` | So sánh công việc/kỹ năng/tradeoff bằng đoạn văn. Không tự hiện simulation hay kết quả cá nhân, không suy ra “bạn chưa làm bài” chỉ vì lượt này không cần đọc bài làm. Dữ liệu đối chiếu được thu gọn. |
| `Dựa trên kết quả của tôi, so sánh frontend và backend nhưng đừng đề xuất simulation.` | Diễn giải có liên hệ nhiệm vụ đã làm, citation nhỏ; chi tiết kết quả đóng mặc định. Không có card simulation vì đã yêu cầu loại bỏ. Không suy rộng một bài thành năng lực nghề nghiệp. |
| `Cho tôi tài nguyên học của hướng đầu tiên` | Bám frontend nếu ngay trước đó thứ tự là frontend rồi backend; giải thích vì sao nguồn hữu ích. |
| `so sánh các hướng đi devops, AI cho tôi tham khảo với nhé` | Phải nói về DevOps và AI; không thay bằng backend/cybersecurity. Có thử knowledge + web. Nếu web lỗi, nói rõ giới hạn và có thể giải thích bằng kiến thức tổng quát, không giả danh kết quả web. |
| `Tìm nguồn học Spring Boot backend trên internet.` | Có nguồn W khi grounding thành công. Nếu RATE_LIMITED: thấy thông báo quota, không nói các link catalog là kết quả search mới. |
| `Giải thích bước thứ hai của kế hoạch đó cụ thể hơn` | Sau lượt có draft: gọi LLM với draft trong session, giải thích bước đang nói tới. Không tự lưu plan. |
| `Compare DevOps and AI engineering: daily work, prerequisites and tradeoffs.` | Cùng nguyên tắc bằng tiếng Anh; không suy diễn năng lực cá nhân từ bài frontend/backend. |

### RAG test ở đâu?

**RAG không đồng nghĩa search web.** Kiểm tra bằng câu như `Idempotency trong backend là gì? Giải thích theo tài liệu đã duyệt.` hoặc `Placeholder có thay thế label của biểu mẫu không? Giải thích theo tài liệu.` Các chủ đề này có trong corpus draft đã chuẩn bị. Xem trạng thái corpus trước khi diễn giải kết quả:

Câu hỏi khái niệm này không cần gửi điểm bài làm cho model và không tự thêm card simulation/nguồn học. Nếu hỏi thêm “gợi ý nguồn học và bài thực hành” thì mới cần các công cụ tương ứng.

- **Chưa publish:** không được có citation K giả. Model vẫn có thể trả lời kiến thức tổng quát, nói chưa có tài liệu phù hợp; đó chưa phải chứng minh RAG truy xuất thành công.
- **Sau khi review và publish:** câu trả lời có citation `[K…]`; bấm mở, đối chiếu passage/section/version với câu trả lời. Trong trace nội bộ, `retrievalStatus=HYBRID` xác nhận có vector + lexical; `LEXICAL_ONLY`/`EMBEDDING_UNAVAILABLE` cho biết đang dùng phương án dự phòng. Citation K một mình chưa chứng minh vector đã hoạt động.
- **Câu không có trong tài liệu:** hỏi nội dung một tài liệu chưa nạp; Syn phải nói không có tài liệu, không gán kiến thức model thành nội dung tài liệu đó.

Quy trình publish và ca lỗi/consent ở bên dưới. Không tự coi nhãn AI-assisted là RAG thành công hay web thành công.

Mở http://localhost:8080/dashboard trong stack demo local. Bật consent AI nếu chưa bật, chọn ngôn ngữ VI, bật memory. Xóa cuộc hội thoại cũ trước nhóm test mới để không dùng transcript/cache cũ; trong mỗi nhóm follow-up giữ nguyên hội thoại. Không cần làm lại assessment/simulation đã hoàn tất.

## Những ca nên thử trước

| Bước | Thao tác/câu hỏi | Kết quả cần quan sát |
|---|---|---|
| 1 | Bấm **Suggest a learning resource** | Quick action có thể hiện **Standard guidance**, đây là chủ đích. Khi catalog có tài nguyên phù hợp phải có card/link, không chỉ nói chung chung. |
| 2 | `Tôi hứng thú với backend, hãy gợi ý nguồn học, simulation và phác thảo kế hoạch 2 tuần để tôi trải nghiệm thêm.` | Cùng một lượt có resources thuộc backend, simulation và bản nháp kế hoạch. Không tự lưu kế hoạch, không kết luận bạn phù hợp nghề nào. Text có thể AI-assisted hoặc Standard nếu provider lỗi; cards/draft vẫn có. |
| 3 | Hội thoại mới: `So sánh frontend và backend dựa trên kết quả của tôi.` Sau đó: `Cho tôi tài nguyên học của hướng đầu tiên.` | Comparison frontend trước backend; follow-up lấy frontend. Thử “hướng thứ hai” ngay sau một comparison mới để kiểm tra backend. Memory hiện theo ngữ cảnh gần nhất, không phải lịch sử ngữ nghĩa vô hạn. |
| 4 | Sau khi Syn vừa hiển thị nhiều tài nguyên: `Tôi muốn học tài nguyên thứ hai, hãy phác thảo kế hoạch 1 tuần.` | Kế hoạch dùng skill của tài nguyên thứ hai; link được tải lại từ catalog REVIEWED. Nếu chỉ có một tài nguyên thì không có mục thứ hai để chọn. |
| 5 | `Tìm cho tôi nguồn học Spring Boot backend trên internet.` | Khi search thành công: panel nguồn web có link, nhãn chưa review và thời điểm truy xuất; Google Search Suggestions có thể xuất hiện bên dưới. Nếu provider không trả được nguồn, UI báo không khả dụng và vẫn giữ nguồn nội bộ, không bịa link. |
| 6 | Hội thoại mới: `I enjoy backend. Suggest a resource, a simulation and a two-week practice plan.` | Resource/simulation/plan vẫn đủ; diễn giải bằng tiếng Anh nếu chọn EN hoặc Auto. |
| 7 | `Tôi làm tốt một bài backend, vậy tôi có năng lực backend tốt và chắc chắn hợp nghề này đúng không?` | Không xác nhận kết luận năng lực/nghề từ một bài. Liên hệ đúng nhiệm vụ đã làm, nói giới hạn bằng chứng và gợi ý trải nghiệm tiếp. Guard hiện là heuristic; nếu có suy diễn quá mức hãy gửi lại ví dụ. |

## Kiểm tra RAG sau khi duyệt corpus

Hiện corpus draft chưa được publish. Trước publish, model có thể giải thích kiến thức tổng quát nhưng không được gán citation K giả; không lấy thiếu citation làm bằng chứng rằng vector retrieval hỏng.

1. Đọc [bản corpus dễ review](../data/knowledge/corpus-review.vi.md), sửa nội dung/provenance nếu cần. Tạo `corpus.approved.json` riêng; chỉ đặt `reviewed: true` và reviewer thật cho tài liệu đã đọc và duyệt.
2. Từ thư mục dự án: `powershell -ExecutionPolicy Bypass -File scripts/Publish-SynKnowledge.ps1 -Manifest docs/data/knowledge/corpus.approved.json`.
3. Hỏi `Idempotency trong backend là gì?` hoặc một câu đúng chủ đề trong tài liệu đã publish. Mong đợi references **K**, xem được đoạn trích/section/version; không trộn với điểm bài làm.
4. Hỏi điều không có trong corpus: Syn phải thể hiện giới hạn nguồn. Kiểm tra thủ công citation có thực sự hỗ trợ câu trả lời. Mock tests không chứng minh điều này.

## Tình huống lỗi và consent

Ở lần kiểm tra ngày 2026-09-29, Google Search grounding trên key hiện tại trả HTTP 429 / RESOURCE_EXHAUSTED. Nếu chưa có nguồn web, kiểm tra quota của project tại Google AI Studio; restart Docker không tăng quota. Chưa có kiểm chứng live web thành công trên key này, dù generation chat vẫn có lượt AI_READY.

- Tắt consent rồi gửi yêu cầu nguồn web: không gọi external provider; nguồn nội bộ vẫn dùng được. Bật lại sau test.
- `SYN_WEB_ENABLED=false` trong `.env`, sau đó recreate backend: web báo không khả dụng; các card catalog vẫn hoạt động. Khi bật lại cần recreate backend lần nữa.
- Không nhấn Save khi chỉ đang kiểm tra draft. Save là thao tác ghi thực, cần xác nhận riêng trong giao diện.
- Standard trên quick action là bình thường. Standard trên free text: xem `reasonCode` trong Network response; phân biệt timeout, rate limit, provider unavailable và output invalid. Không cần down/up toàn bộ stack hoặc xóa volume.

## Chạy bản mới và gửi nhận xét

Nếu sửa source: `docker compose up -d --build backend frontend`, rồi `docker compose restart gateway`. Nếu chỉ đổi `.env`: `docker compose up -d --force-recreate backend`. Gateway mặc định 8080; kiểm tra `GATEWAY_PORT` nếu máy đang dùng port khác. Không dùng `down -v` vì sẽ mất dữ liệu.

Khi gửi nhận xét, cung cấp câu hỏi, 1–2 lượt trước đó, text/cards mong đợi và thực tế, nhãn AI-assisted/Standard, `reasonCode` nếu có. Không gửi API key. Ưu tiên nhận xét độ phù hợp của nguồn, kế hoạch có thực hiện được không, follow-up có bám đúng chủ đề và câu nào suy diễn vượt bằng chứng.
