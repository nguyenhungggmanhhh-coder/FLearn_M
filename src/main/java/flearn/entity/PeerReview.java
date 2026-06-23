package flearn.entity;

import flearn.enums.PeerReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

/**
 * PeerReview – Chấm chéo bài tự luận trong phạm vi lớp học.
 *
 * Sau khi deadline nộp bài kết thúc, hệ thống tự động phân công:
 * mỗi sinh viên chấm N bài của bạn cùng lớp (ngẫu nhiên, tránh chấm bài chính mình).
 * Điểm cuối = trung bình điểm peer. Teacher có thể override.
 * Độ lệch (deviationScore) so với điểm TB lớp giúp phát hiện chấm lệch.
 */
@Entity
@Table(name = "[PeerReviews]")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PeerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "[PeerReviewID]")
    private Integer id;

    /** Bài nộp được chấm (QuizResult của sinh viên bị chấm). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[RevieweeResultID]", nullable = false)
    private QuizResult revieweeResult;

    /** Sinh viên thực hiện chấm bài. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[ReviewerID]", nullable = false)
    private User reviewer;

    /** Điểm người chấm cho (0.0 – 10.0). null = chưa chấm. */
    @Column(name = "[Score]")
    private Double score;

    /** Nhận xét của người chấm. */
    @Column(name = "[Comment]", columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    /** Trạng thái: PENDING / SUBMITTED / OVERRIDDEN (teacher đã sửa điểm). */
    @Enumerated(EnumType.STRING)
    @Column(name = "[Status]", nullable = false, length = 20)
    @Builder.Default
    private PeerReviewStatus status = PeerReviewStatus.PENDING;

    /** Hạn chót để sinh viên nộp bài chấm. */
    @Column(name = "[Deadline]")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deadline;

    /** Thời điểm phân công. */
    @Column(name = "[AssignedAt]", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date assignedAt;

    /** Thời điểm sinh viên nộp bài chấm. */
    @Column(name = "[SubmittedAt]")
    @Temporal(TemporalType.TIMESTAMP)
    private Date submittedAt;

    /** Độ lệch điểm so với điểm TB lớp (tính sau khi tất cả peer nộp xong). */
    @Column(name = "[DeviationFromMean]")
    private Double deviationFromMean;

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) assignedAt = new Date();
        if (status == null) status = PeerReviewStatus.PENDING;
    }
}
