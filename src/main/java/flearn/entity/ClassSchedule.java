package flearn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.Date;

/**
 * ClassSchedule – Lịch học tuần lặp lại của một lớp.
 * Admin tạo lịch: thứ mấy, giờ mấy, phòng/link gì.
 * Hệ thống tự gửi email nhắc nhở trước 1 ngày và 2 tiếng.
 */
@Entity
@Table(name = "[ClassSchedules]")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "[ScheduleID]")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[ClassID]", nullable = false)
    private Classroom classroom;

    /**
     * Thứ trong tuần theo chuẩn Java DayOfWeek (1=CN, 2=T2, ..., 7=T7).
     * Sử dụng java.time.DayOfWeek.getValue() để map.
     */
    @Column(name = "[DayOfWeek]", nullable = false)
    private Integer dayOfWeek;

    /** Giờ bắt đầu buổi học (VD: 08:00). */
    @Column(name = "[StartTime]", nullable = false)
    private LocalTime startTime;

    /** Giờ kết thúc buổi học (VD: 10:00). */
    @Column(name = "[EndTime]")
    private LocalTime endTime;

    /** Phòng học hoặc link Google Meet/Zoom. */
    @Column(name = "[RoomOrLink]", length = 500)
    private String roomOrLink;

    /** Mô tả ngắn về nội dung buổi học (tùy chọn). */
    @Column(name = "[Note]", length = 500)
    private String note;

    /** Gửi email nhắc trước 1 ngày. */
    @Column(name = "[RemindOneDayBefore]", nullable = false)
    @Builder.Default
    private Boolean remindOneDayBefore = true;

    /** Gửi email nhắc trước 2 tiếng. */
    @Column(name = "[RemindTwoHoursBefore]", nullable = false)
    @Builder.Default
    private Boolean remindTwoHoursBefore = true;

    /** Lịch còn hoạt động hay không (ẩn lịch mà không xóa). */
    @Column(name = "[IsActive]", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
        if (remindOneDayBefore == null) remindOneDayBefore = true;
        if (remindTwoHoursBefore == null) remindTwoHoursBefore = true;
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
