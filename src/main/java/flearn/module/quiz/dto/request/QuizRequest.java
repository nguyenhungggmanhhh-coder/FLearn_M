package flearn.module.quiz.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRequest {
    @NotBlank(message = "Tên bài kiểm tra không được để trống.")
    @Size(max = 200, message = "Tên bài kiểm tra không được vượt quá 200 ký tự.")
    private String title;

    @Size(max = 2000, message = "Mô tả bài kiểm tra quá dài.")
    private String description;

    @Min(value = 1, message = "Thời gian làm bài tối thiểu là 1 phút.")
    @Max(value = 600, message = "Thời gian làm bài tối đa là 600 phút.")
    private Integer timeLimitMinutes;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date deadline;

    private Boolean shuffleQuestions;
    private Boolean shuffleAnswers;

    @Min(value = 1, message = "Số lần làm bài tối thiểu là 1.")
    @Max(value = 50, message = "Số lần làm bài tối đa là 50.")
    private Integer maxAttempts;

    @Min(value = 0, message = "Mốc thời gian video không được âm.")
    private Integer videoTimestamp; // giây

    /** [FEAT-01] Số câu hỏi được rút ngẫu nhiên từ ngân hàng. null = dùng tất cả. */
    @Min(value = 1, message = "Số câu rút ngẫu nhiên tối thiểu là 1.")
    @Max(value = 1000, message = "Số câu rút ngẫu nhiên tối đa là 1000.")
    private Integer questionPoolSize;
}
