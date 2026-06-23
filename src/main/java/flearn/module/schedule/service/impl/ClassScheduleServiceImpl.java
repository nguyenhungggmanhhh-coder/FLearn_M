package flearn.module.schedule.service.impl;

import flearn.common.exception.BusinessException;
import flearn.entity.ClassSchedule;
import flearn.entity.Classroom;
import flearn.module.schedule.dto.request.ClassScheduleRequest;
import flearn.module.schedule.dto.response.ClassScheduleResponse;
import flearn.module.schedule.service.ClassScheduleService;
import flearn.repository.ClassScheduleRepository;
import flearn.repository.ClassroomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class ClassScheduleServiceImpl implements ClassScheduleService {

    private final ClassScheduleRepository scheduleRepository;
    private final ClassroomRepository     classroomRepository;

    @Override
    public List<ClassScheduleResponse> getSchedulesByClass(Integer classId) {
        Classroom classroom = findClassById(classId);
        return scheduleRepository
                .findByClassroomAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(classroom)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ClassScheduleResponse getScheduleById(Integer scheduleId) {
        return toResponse(findScheduleById(scheduleId));
    }

    @Override
    @Transactional
    public void createSchedule(Integer classId, ClassScheduleRequest request) {
        Classroom classroom = findClassById(classId);
        ClassSchedule schedule = ClassSchedule.builder()
                .classroom(classroom)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .roomOrLink(request.getRoomOrLink())
                .note(request.getNote())
                .remindOneDayBefore(Boolean.TRUE.equals(request.getRemindOneDayBefore()))
                .remindTwoHoursBefore(Boolean.TRUE.equals(request.getRemindTwoHoursBefore()))
                .isActive(Boolean.TRUE.equals(request.getIsActive()))
                .build();
        scheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void updateSchedule(Integer scheduleId, ClassScheduleRequest request) {
        ClassSchedule schedule = findScheduleById(scheduleId);
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setRoomOrLink(request.getRoomOrLink());
        schedule.setNote(request.getNote());
        schedule.setRemindOneDayBefore(Boolean.TRUE.equals(request.getRemindOneDayBefore()));
        schedule.setRemindTwoHoursBefore(Boolean.TRUE.equals(request.getRemindTwoHoursBefore()));
        scheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void toggleScheduleActive(Integer scheduleId) {
        ClassSchedule schedule = findScheduleById(scheduleId);
        schedule.setIsActive(!Boolean.TRUE.equals(schedule.getIsActive()));
        scheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(Integer scheduleId) {
        scheduleRepository.delete(findScheduleById(scheduleId));
    }

    // ─────────────── Helpers ───────────────

    private ClassSchedule findScheduleById(Integer id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lịch học."));
    }

    private Classroom findClassById(Integer classId) {
        return classroomRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lớp học."));
    }

    private ClassScheduleResponse toResponse(ClassSchedule s) {
        return ClassScheduleResponse.builder()
                .id(s.getId())
                .classId(s.getClassroom().getClassId())
                .className(s.getClassroom().getClassName())
                .dayOfWeek(s.getDayOfWeek())
                .dayOfWeekLabel(ClassScheduleResponse.dayLabel(s.getDayOfWeek()))
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .roomOrLink(s.getRoomOrLink())
                .note(s.getNote())
                .remindOneDayBefore(s.getRemindOneDayBefore())
                .remindTwoHoursBefore(s.getRemindTwoHoursBefore())
                .isActive(s.getIsActive())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
