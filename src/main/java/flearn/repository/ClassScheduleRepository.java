package flearn.repository;

import flearn.entity.ClassSchedule;
import flearn.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Integer> {

    /** Lấy tất cả lịch học còn active của một lớp, sắp theo thứ rồi giờ. */
    List<ClassSchedule> findByClassroomAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(Classroom classroom);

    /** Lấy tất cả lịch active toàn hệ thống (dùng cho cron job nhắc nhở). */
    @Query("SELECT s FROM ClassSchedule s WHERE s.isActive = true")
    List<ClassSchedule> findAllActive();

    /** Lấy lịch theo ID và kiểm tra quyền admin. */
    List<ClassSchedule> findByClassroom_ClassId(Integer classId);
}
