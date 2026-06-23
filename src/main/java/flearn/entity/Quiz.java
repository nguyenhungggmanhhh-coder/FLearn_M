package flearn.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import lombok.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "[Quizzes]")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "[QuizID]")
    private Integer quizId;

    @Nationalized
    @Column(name = "[Title]", nullable = false, columnDefinition = "NVARCHAR(200)")
    private String title;

    @Nationalized
    @Column(name = "[Description]", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[LessonID]", nullable = false)
    private Lesson lesson;

    @Column(name = "[Published]", nullable = false)
    @Builder.Default
    private Boolean published = false;

    @Column(name = "[TimeLimitMinutes]")
    private Integer timeLimitMinutes;

    @Column(name = "[Deadline]")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deadline;

    @Column(name = "[ShuffleQuestions]", nullable = false)
    @Builder.Default
    private Boolean shuffleQuestions = false;

    @Column(name = "[ShuffleAnswers]", nullable = false)
    @Builder.Default
    private Boolean shuffleAnswers = false;

    @Column(name = "[MaxAttempts]", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 1;

    /**
     * [FEAT-01] Ngân hàng đề: Số lượng câu hỏi được chọn ngẫu nhiên cho mỗi lần thi.
     * null = lấy tất cả câu hỏi (hành vi cũ, tương thích ngược).
     * VD: 20 = lấy ngẫu nhiên 20 câu từ toàn bộ ngân hàng.
     */
    @Column(name = "[QuestionPoolSize]")
    private Integer questionPoolSize;

    @Column(name = "[VideoTimestamp]")
    private Integer videoTimestamp; // Giây trong video, null = không gate quiz

    @Column(name = "[CreatedAt]", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "[UpdatedAt]")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Question> questions;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = createdAt;
        syncDefaults();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
        syncDefaults();
    }

    private void syncDefaults() {
        if (published == null) {
            published = false;
        }
        if (shuffleQuestions == null) {
            shuffleQuestions = false;
        }
        if (shuffleAnswers == null) {
            shuffleAnswers = false;
        }
        if (maxAttempts == null || maxAttempts < 1) {
            maxAttempts = 1;
        }
    }
}
