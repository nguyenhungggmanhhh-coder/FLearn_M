package flearn.module.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {
    private Integer quizId;
    private String title;
    private String description;
    private Integer lessonId;
    private String lessonTitle;
    private Integer classId;
    private Boolean published;
    private Integer timeLimitMinutes;
    private Date deadline;
    private Boolean shuffleQuestions;
    private Boolean shuffleAnswers;
    private Integer maxAttempts;
    private Integer videoTimestamp; // giây trong video, null = không gate
    private boolean deadlinePassed;
    private long submittedAttempts;
    /** [FEAT-01] Số câu hỏi rút ngẫu nhiên; null = dùng tất cả. */
    private Integer questionPoolSize;
    private List<QuestionResponse> questions;
}
