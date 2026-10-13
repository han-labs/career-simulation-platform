package edu.hcmute.careersim.simulation.mapper;

import edu.hcmute.careersim.simulation.dto.SubmitAttemptResponse;
import edu.hcmute.careersim.simulation.dto.TaskOutcomeDto;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Response-only localization after evaluation. Never used by public task queries or scoring. */
@Component
public class SimulationResultLocaleMapper {
    private static final Map<String, String> VI =
            Map.ofEntries(
                    Map.entry(
                            "HTTP 404 indicates that the requested resource could not be found.",
                            "HTTP 404 cho biết không tìm thấy tài nguyên được yêu cầu."),
                    Map.entry(
                            "SELECT * FROM users is the SQL statement that retrieves all rows and columns.",
                            "SELECT * FROM users là câu lệnh SQL lấy tất cả các dòng và cột."),
                    Map.entry(
                            "HTTP 400 communicates that the submitted request is invalid or incomplete.",
                            "HTTP 400 cho biết yêu cầu gửi lên không hợp lệ hoặc chưa đầy đủ."),
                    Map.entry(
                            "One h1 for the page followed by h2 section headings creates a clear semantic outline.",
                            "Một h1 cho trang, sau đó là h2 cho các phần, tạo cấu trúc ngữ nghĩa rõ ràng."),
                    Map.entry(
                            "A native button provides keyboard activation, focus behavior, and semantics by default.",
                            "Phần tử button chuẩn sẵn có hỗ trợ kích hoạt bằng bàn phím, tiêu điểm và ngữ nghĩa."),
                    Map.entry(
                            "Text linked to the field and an error summary communicate the problem without relying on color alone.",
                            "Nội dung lỗi liên kết với trường và phần tổng hợp lỗi giúp thông báo vấn đề mà không chỉ dựa vào màu sắc."),
                    Map.entry(
                            "Measure and inspect the affected rows before choosing a documented cleaning rule.",
                            "Định lượng và kiểm tra các dòng bị ảnh hưởng trước khi chọn quy tắc làm sạch được ghi lại rõ ràng."),
                    Map.entry(
                            "A documented rule using a stable key and timestamp can be reviewed and repeated.",
                            "Quy tắc được ghi lại, sử dụng khóa ổn định và thời gian cập nhật, có thể được kiểm tra và thực hiện lại."),
                    Map.entry(
                            "Re-running explicit quality checks makes the result measurable and reproducible.",
                            "Chạy lại các kiểm tra chất lượng cụ thể giúp đo lường và tái lập kết quả."),
                    Map.entry(
                            "Values immediately below, on, and above each boundary expose off-by-one errors.",
                            "Giá trị ngay dưới, đúng tại và ngay trên mỗi biên giúp phát hiện lỗi lệch một đơn vị."),
                    Map.entry(
                            "Changed, high-impact payment behavior carries more immediate product risk.",
                            "Luồng thanh toán vừa thay đổi và có tác động lớn mang rủi ro trực tiếp cao hơn cho sản phẩm."),
                    Map.entry(
                            "The server logs and validation boundary help distinguish a missing validation rule from an internal defect.",
                            "Nhật ký máy chủ và ranh giới kiểm tra dữ liệu giúp phân biệt thiếu quy tắc kiểm tra với lỗi nội bộ."),
                    Map.entry(
                            "Containment plus evidence preservation reduces immediate risk without destroying investigation data.",
                            "Khoanh vùng kết hợp bảo toàn bằng chứng giúp giảm rủi ro trước mắt mà không phá hủy dữ liệu điều tra."),
                    Map.entry(
                            "A controlled snapshot and documented chain of custody preserve evidence for later analysis.",
                            "Bản chụp có kiểm soát và hồ sơ bàn giao, xử lý giúp bảo toàn bằng chứng cho phân tích sau này."),
                    Map.entry(
                            "Revoking and rotating the key limits further abuse; history and logs still need investigation.",
                            "Thu hồi và thay khóa giúp hạn chế lạm dụng tiếp diễn; vẫn cần kiểm tra lịch sử và nhật ký."));

    public SubmitAttemptResponse localize(SubmitAttemptResponse result) {
        return new SubmitAttemptResponse(
                result.attemptId(),
                result.correctCount(),
                result.totalTasks(),
                result.percentage(),
                result.outcomes().stream().map(this::localizeOutcome).toList());
    }

    private TaskOutcomeDto localizeOutcome(TaskOutcomeDto outcome) {
        String translated = outcome.explanation() == null ? null : VI.get(outcome.explanation());
        return new TaskOutcomeDto(
                outcome.taskId(),
                outcome.title(),
                outcome.selectedOption(),
                outcome.isCorrect(),
                outcome.explanation(),
                translated);
    }
}
