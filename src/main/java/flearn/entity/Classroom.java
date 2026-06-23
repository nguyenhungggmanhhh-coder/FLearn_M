package flearn.entity;

import flearn.enums.ClassStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "[Classes]")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Classroom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "[ClassID]")
    private Integer classId;

    @Nationalized
    @Column(name = "[ClassName]", nullable = false, length = 100)
    private String className;

    @Nationalized
    @Column(name = "[Description]", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[TeacherID]", nullable = true)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[CourseID]", nullable = true)
    private Course course;

    @Column(name = "[InviteCode]", unique = true, nullable = false, length = 10)
    private String inviteCode;

    /**
     * [FEAT-04] Kiểm soát hiển thị mã mời:
     * - inviteCodeVisible = true: Admin/Teacher đã bật hiển
     * - inviteCodeGeneratedAt: Thời điểm bật – frontend tự ẩn sau 15 phút
     * Logic hiển: visible=true AND now &lt; generatedAt + 15 phút
     */
    @Column(name = "[InviteCodeVisible]", nullable = false)
    @Builder.Default
    private Boolean inviteCodeVisible = false; // Mặc định ẩn, chỉ bật khi cần show

    @Column(name = "[InviteCodeGeneratedAt]")
    @Temporal(TemporalType.TIMESTAMP)
    private Date inviteCodeGeneratedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "[Status]", length = 20)
    @Builder.Default
    private ClassStatus status = ClassStatus.ACTIVE;

    @Column(name = "[IsActive]", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "[IsJoinable]", nullable = false)
    @Builder.Default
    private Boolean isJoinable = true;

    @Column(name = "[StartDate]")
    private LocalDate startDate;

    @Column(name = "[EndDate]")
    private LocalDate endDate;

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
        syncStatusFields();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
        syncStatusFields();
    }

    public String getClassCode() {
        return inviteCode;
    }

    public void setClassCode(String classCode) {
        this.inviteCode = classCode;
    }

    private void syncStatusFields() {
        if (status == null) {
            status = Boolean.FALSE.equals(isActive) ? ClassStatus.INACTIVE : ClassStatus.ACTIVE;
        }
        isActive = status == ClassStatus.ACTIVE;
    }
}
