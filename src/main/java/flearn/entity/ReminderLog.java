package flearn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

/**
 * ReminderLog – Lưu lịch sử email nhắc nhở đã gửi.
 * UniqueConstraint đảm bảo mỗi (lịch, sinh viên, loại nhắc, ngày học) chỉ gửi 1 lần.
 */
@Entity
@Table(name = "[ReminderLogs]",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_ReminderLog",
                columnNames = {"[ScheduleID]", "[UserID]", "[ReminderType]", "[ScheduledDate]"}
        ))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "[LogID]")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[ScheduleID]", nullable = false)
    private ClassSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[UserID]", nullable = false)
    private User user;

    /**
     * Loại nhắc nhở:
     * - "1DAY" = nhắc trước 1 ngày
     * - "2HOURS" = nhắc trước 2 tiếng
     */
    @Column(name = "[ReminderType]", nullable = false, length = 20)
    private String reminderType;

    /** Ngày diễn ra buổi học được nhắc (không phải ngày gửi email). */
    @Column(name = "[ScheduledDate]", nullable = false)
    private LocalDate scheduledDate;

    /** Thời điểm email thực sự được gửi. */
    @Column(name = "[SentAt]")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentAt;

    /** Email gửi đến (để trace nếu user đổi email sau). */
    @Column(name = "[SentToEmail]", length = 100)
    private String sentToEmail;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) sentAt = new Date();
    }
}
