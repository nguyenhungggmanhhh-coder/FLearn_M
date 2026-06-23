package flearn.common.service;

import flearn.entity.*;
import flearn.enums.ClassStatus;
import flearn.enums.EnrollmentStatus;
import flearn.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * ReminderService – Gửi email nhắc nhở lịch học.
 *
 * Cron chạy mỗi 5 phút, kiểm tra:
 *   1. Tìm buổi học tiếp theo của mỗi lịch học active
 *   2. Nếu buổi học còn đúng ~24 giờ → gửi nhắc "1 ngày trước"
 *   3. Nếu buổi học còn đúng ~2 tiếng → gửi nhắc "2 tiếng trước"
 *   4. Mỗi (lịch, sinh viên, loại nhắc, ngày học) chỉ gửi 1 lần (ReminderLog)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final String TYPE_1DAY   = "1DAY";
    private static final String TYPE_2HOURS = "2HOURS";
    // Window cho phép: ±10 phút xung quanh mốc nhắc
    private static final long WINDOW_MINUTES = 10;

    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository    enrollmentRepository;
    private final ReminderLogRepository   reminderLogRepository;
    private final EmailService            emailService;

    /**
     * Cron mỗi 5 phút kiểm tra và gửi email nhắc nhở.
     * fixedDelay tránh chạy chồng nếu lần trước chưa xong.
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void checkAndSendReminders() {
        List<ClassSchedule> schedules = classScheduleRepository.findAllActive();
        if (schedules.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        log.debug("[Reminder] Cron triggered at {}, checking {} schedules", now, schedules.size());

        for (ClassSchedule schedule : schedules) {
            try {
                processSchedule(schedule, now);
            } catch (Exception ex) {
                log.error("[Reminder] Error processing schedule id={}: {}", schedule.getId(), ex.getMessage());
            }
        }
    }

    private void processSchedule(ClassSchedule schedule, LocalDateTime now) {
        // Tính buổi học tiếp theo trong tuần
        LocalDateTime nextClass = nextOccurrence(schedule.getDayOfWeek(), schedule.getStartTime(), now);

        // Tính khoảng cách giờ tới buổi học
        long minutesUntilClass = java.time.Duration.between(now, nextClass).toMinutes();

        long oneDayMinutes   = 24 * 60;
        long twoHourMinutes  = 2 * 60;

        boolean sendOneDay   = schedule.getRemindOneDayBefore()
                && minutesUntilClass >= (oneDayMinutes - WINDOW_MINUTES)
                && minutesUntilClass <= (oneDayMinutes + WINDOW_MINUTES);

        boolean sendTwoHours = schedule.getRemindTwoHoursBefore()
                && minutesUntilClass >= (twoHourMinutes - WINDOW_MINUTES)
                && minutesUntilClass <= (twoHourMinutes + WINDOW_MINUTES);

        if (!sendOneDay && !sendTwoHours) return;

        // Lấy tất cả sinh viên active của lớp này
        List<User> students = enrollmentRepository
                .findByClassRoomAndStatus(schedule.getClassroom(), EnrollmentStatus.ACTIVE)
                .stream()
                .map(Enrollment::getStudent)
                .toList();

        LocalDate classDate = nextClass.toLocalDate();

        for (User student : students) {
            if (sendOneDay) {
                sendIfNotSent(schedule, student, TYPE_1DAY, classDate, nextClass);
            }
            if (sendTwoHours) {
                sendIfNotSent(schedule, student, TYPE_2HOURS, classDate, nextClass);
            }
        }
    }

    private void sendIfNotSent(ClassSchedule schedule, User student,
                                String type, LocalDate classDate, LocalDateTime nextClass) {
        if (student.getEmail() == null || student.getEmail().isBlank()) return;

        boolean alreadySent = reminderLogRepository.existsByScheduleAndUserAndReminderTypeAndScheduledDate(
                schedule, student, type, classDate);
        if (alreadySent) return;

        // Gửi email
        String subject = buildSubject(type, schedule);
        String body    = buildBody(type, schedule, student, nextClass);

        try {
            emailService.sendEmail(student.getEmail(), subject, body);

            // Ghi log để không gửi lại
            reminderLogRepository.save(ReminderLog.builder()
                    .schedule(schedule)
                    .user(student)
                    .reminderType(type)
                    .scheduledDate(classDate)
                    .sentToEmail(student.getEmail())
                    .build());

            log.info("[Reminder] Sent {} reminder → {} for schedule {}", type, student.getEmail(), schedule.getId());
        } catch (Exception ex) {
            log.error("[Reminder] Failed to send email to {}: {}", student.getEmail(), ex.getMessage());
        }
    }

    /**
     * Tính buổi học tiếp theo trong tuần từ dayOfWeek + startTime.
     * Nếu hôm nay đúng thứ đó nhưng giờ đã qua → lấy tuần sau.
     */
    private LocalDateTime nextOccurrence(int targetDayOfWeek, LocalTime startTime, LocalDateTime now) {
        // Java DayOfWeek: 1=Mon, ..., 7=Sun. DB lưu: 2=T2(Mon), ..., 7=T7(Sat), 1=CN(Sun)
        // Chuẩn hóa sang Java DayOfWeek ISO: Mon=1...Sun=7
        int isoDow = targetDayOfWeek == 1 ? 7 : targetDayOfWeek - 1;
        DayOfWeek targetDow = DayOfWeek.of(isoDow);

        LocalDate today = now.toLocalDate();
        LocalDate candidate = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(targetDow));
        LocalDateTime candidateDT = candidate.atTime(startTime);

        // Nếu thời điểm candidate đã qua → lấy tuần sau
        if (!candidateDT.isAfter(now)) {
            candidate = candidate.plusWeeks(1);
            candidateDT = candidate.atTime(startTime);
        }
        return candidateDT;
    }

    private String buildSubject(String type, ClassSchedule schedule) {
        String timeLabel = TYPE_1DAY.equals(type) ? "1 ngày" : "2 tiếng";
        return String.format("[FLearn] Nhắc nhở: Lớp \"%s\" sẽ bắt đầu sau %s",
                schedule.getClassroom().getClassName(), timeLabel);
    }

    private String buildBody(String type, ClassSchedule schedule, User student, LocalDateTime nextClass) {
        String timeLabel = TYPE_1DAY.equals(type) ? "1 ngày nữa" : "2 tiếng nữa";
        String roomOrLink = schedule.getRoomOrLink() != null ? schedule.getRoomOrLink() : "Chưa cập nhật";
        String note = schedule.getNote() != null ? "\nNội dung: " + schedule.getNote() : "";

        return String.format(
            "Xin chào %s,\n\n" +
            "Lớp học \"%s\" của bạn sẽ bắt đầu sau %s.\n\n" +
            "📅 Ngày: %s\n" +
            "⏰ Giờ: %s – %s\n" +
            "📍 Phòng/Link: %s%s\n\n" +
            "Hãy chuẩn bị sẵn sàng để tham gia buổi học!\n\n" +
            "Trân trọng,\nFLearn System",
            student.getFullName(),
            schedule.getClassroom().getClassName(),
            timeLabel,
            nextClass.toLocalDate(),
            schedule.getStartTime(),
            schedule.getEndTime() != null ? schedule.getEndTime().toString() : "?",
            roomOrLink,
            note
        );
    }
}
