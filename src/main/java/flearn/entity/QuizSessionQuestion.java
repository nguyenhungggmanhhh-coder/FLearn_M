package flearn.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * QuizSessionQuestion – Lưu bộ câu hỏi được rút ngẫu nhiên cho mỗi lần thi của sinh viên.
 *
 * Khi sinh viên bắt đầu quiz có questionPoolSize (VD: 20 câu từ ngân hàng 100 câu),
 * hệ thống rút ngẫu nhiên và lưu vào đây. Mỗi lần nộp bài chỉ chấm dựa trên
 * bộ câu này (không random lại), đảm bảo nhất quán và công bằng.
 */
@Entity
@Table(name = "[QuizSessionQuestions]",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_QuizSessionQuestion",
                columnNames = {"[ResultID]", "[QuestionID]"}
        ))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizSessionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "[SessionQuestionID]")
    private Integer id;

    /** Kết quả thi (session) của sinh viên. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[ResultID]", nullable = false)
    private QuizResult result;

    /** Câu hỏi được chọn cho session này. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[QuestionID]", nullable = false)
    private Question question;

    /** Thứ tự hiển thị câu hỏi (sau khi shuffle). */
    @Column(name = "[DisplayOrder]", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
