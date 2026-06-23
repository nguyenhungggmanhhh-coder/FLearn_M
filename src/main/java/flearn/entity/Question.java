package flearn.entity;

import flearn.enums.QuestionType;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import lombok.*;

@Entity
@Table(name = "[Questions]")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "[QuestionID]")
    private Integer questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[QuizID]", nullable = false)
    private Quiz quiz;

    @Enumerated(EnumType.STRING)
    @Column(name = "[QuestionType]", nullable = false, length = 30)
    @Builder.Default
    private QuestionType type = QuestionType.MULTIPLE_CHOICE;

    @Nationalized
    @Column(name = "[QuestionText]", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String questionText;

    @Nationalized
    @Column(name = "[OptionA]", columnDefinition = "NVARCHAR(500)")
    private String optionA;

    @Nationalized
    @Column(name = "[OptionB]", columnDefinition = "NVARCHAR(500)")
    private String optionB;

    @Nationalized
    @Column(name = "[OptionC]", columnDefinition = "NVARCHAR(500)")
    private String optionC;

    @Nationalized
    @Column(name = "[OptionD]", columnDefinition = "NVARCHAR(500)")
    private String optionD;

    /**
     * Đáp án đúng:
     * - MULTIPLE_CHOICE: "A", "B", "C" hoặc "D"
     * - TRUE_FALSE: "TRUE" hoặc "FALSE"
     * - MULTI_SELECT: [FEAT-02] đa đáp án, VD: "A,C" hoặc "A,B,D"
     * - ESSAY: [FEAT-02] null hoặc trống (chấm thủ công)
     */
    @Column(name = "[CorrectAnswer]", length = 50)
    private String correctAnswer;

    /** [FEAT-02] Đáp án mẫu cho câu tự luận – giáo viên nhập, dùng tham khảo khi chấm. */
    @Column(name = "[ModelAnswer]", columnDefinition = "NVARCHAR(MAX)")
    private String modelAnswer;

    /** [FEAT-02] Trọng số điểm câu hỏi (mặc định 1.0). */
    @Column(name = "[ScoreWeight]")
    @Builder.Default
    private Double scoreWeight = 1.0;

    @Column(name = "[OrderIndex]")
    @Builder.Default
    private Integer orderIndex = 0;

    @PrePersist
    @PreUpdate
    protected void syncDefaults() {
        if (type == null) {
            type = QuestionType.MULTIPLE_CHOICE;
        }
        if (orderIndex == null) {
            orderIndex = 0;
        }
        if (scoreWeight == null) {
            scoreWeight = 1.0;
        }
        // ESSAY không cần correctAnswer – cho phép null
    }
}
