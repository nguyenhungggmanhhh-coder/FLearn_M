package flearn.module.quiz.dto.request;

import flearn.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {
    @NotNull(message = "Vui lòng chọn loại câu hỏi.")
    private QuestionType type;

    @NotBlank(message = "Nội dung câu hỏi không được để trống.")
    @Size(max = 2000, message = "Nội dung câu hỏi quá dài.")
    private String questionText;

    @Size(max = 500, message = "Đáp án A không được vượt quá 500 ký tự.")
    private String optionA;

    @Size(max = 500, message = "Đáp án B không được vượt quá 500 ký tự.")
    private String optionB;

    @Size(max = 500, message = "Đáp án C không được vượt quá 500 ký tự.")
    private String optionC;

    @Size(max = 500, message = "Đáp án D không được vượt quá 500 ký tự.")
    private String optionD;

    /**
     * Đáp án đúng:
     * - MULTIPLE_CHOICE: "A"|"B"|"C"|"D"
     * - TRUE_FALSE: "TRUE"|"FALSE"
     * - MULTI_SELECT: [FEAT-02] VD "A,C" hoặc "A,B,D"
     * - ESSAY: [FEAT-02] để trống hoặc null
     */
    @Pattern(regexp = "^(A|B|C|D|TRUE|FALSE|([ABCD](,[ABCD]){0,3}))?$",
             message = "Đáp án không hợp lệ. Cho phép: A, B, C, D, TRUE, FALSE, hoặc kết hợp A,B,C cho multi-select.")
    private String correctAnswer;

    /** [FEAT-02] Đáp án mẫu cho câu tự luận. */
    @Size(max = 5000, message = "Đáp án mẫu quá dài.")
    private String modelAnswer;

    private Integer orderIndex;
}
