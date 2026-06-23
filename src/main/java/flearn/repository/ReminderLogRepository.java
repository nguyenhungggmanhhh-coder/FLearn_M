package flearn.repository;

import flearn.entity.ClassSchedule;
import flearn.entity.ReminderLog;
import flearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, Integer> {

    /**
     * Kiểm tra đã gửi email nhắc cho (lịch, sinh viên, loại nhắc, ngày học) chưa.
     * Dùng để tránh gửi trùng.
     */
    boolean existsByScheduleAndUserAndReminderTypeAndScheduledDate(
            ClassSchedule schedule,
            User user,
            String reminderType,
            LocalDate scheduledDate
    );

    Optional<ReminderLog> findByScheduleAndUserAndReminderTypeAndScheduledDate(
            ClassSchedule schedule,
            User user,
            String reminderType,
            LocalDate scheduledDate
    );
}
