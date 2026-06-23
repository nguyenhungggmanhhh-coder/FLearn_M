package flearn.entity;

import flearn.enums.MaterialType;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "[Materials]")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "[MaterialID]")
    private Integer id;

    @Nationalized
    @Column(name = "[Title]", nullable = false, length = 200)
    private String title;

    @Nationalized
    @Column(name = "[Description]", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[LessonID]", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "[Type]", nullable = false, length = 30)
    private MaterialType type;

    @Column(name = "[FilePath]", length = 500)
    private String filePath;

    @Column(name = "[ExternalUrl]", length = 1000)
    private String externalUrl;

    @Column(name = "[Published]", nullable = false)
    @Builder.Default
    private Boolean published = false;

    /** [TIME-GATE] Tài liệu chỉ hiện sau mốc này. null = hiện ngay. */
    @Column(name = "[AvailableFrom]")
    @Temporal(TemporalType.TIMESTAMP)
    private Date availableFrom;

    /** Hạn chồt sinh viên phải hoàn thành xem tài liệu. */
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
        if (published == null) {
            published = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
        if (published == null) {
            published = false;
        }
    }
}
