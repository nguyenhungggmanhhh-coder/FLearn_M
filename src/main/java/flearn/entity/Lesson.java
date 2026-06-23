package flearn.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import lombok.*;
import java.util.Date;

@Entity
@Table(name = "[Lessons]")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "[LessonID]")
    private Integer lessonId;

    @Nationalized
    @Column(name = "[Title]", nullable = false, length = 200)
    private String title;

    @Nationalized
    @Column(name = "[Content]", columnDefinition = "NVARCHAR(MAX)")
    private String content; // Ghi chú hoặc tóm tắt bài học

    @Column(name = "[VideoUrl]", length = 500)
    private String videoUrl; // Lưu link nhúng YouTube hoặc Google Drive

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[ClassID]")
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[RoadmapID]")
    private Roadmap roadmap;

    @Column(name = "[OrderIndex]")
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(name = "[Visible]")
    @Builder.Default
    private Boolean visible = true;

    /**
     * Mốc thời gian bài học được mở cho sinh viên xem.
     * null = mở ngay. Thường gắn với ngày diễn ra buổi học theo ClassSchedule.
     */
    @Column(name = "[AvailableFrom]")
    @Temporal(TemporalType.TIMESTAMP)
    private Date availableFrom;

    /** Hạn chồt sinh viên phải hoàn thành bài học này. */
    @Column(name = "[Deadline]")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deadline;

    @Column(name = "[CreatedAt]", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "[UpdatedAt]")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = createdAt;
        syncLearningFields();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
        syncLearningFields();
    }

    private void syncLearningFields() {
        if (visible == null) {
            visible = true;
        }
        if (orderIndex == null) {
            orderIndex = 0;
        }
        if (classroom == null && roadmap != null) {
            classroom = roadmap.getClassRoom();
        }
    }
}
