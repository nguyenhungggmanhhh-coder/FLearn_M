package flearn.module.schedule.service;

import flearn.module.schedule.dto.request.ClassScheduleRequest;
import flearn.module.schedule.dto.response.ClassScheduleResponse;

import java.util.List;

public interface ClassScheduleService {
    List<ClassScheduleResponse> getSchedulesByClass(Integer classId);
    ClassScheduleResponse getScheduleById(Integer scheduleId);
    void createSchedule(Integer classId, ClassScheduleRequest request);
    void updateSchedule(Integer scheduleId, ClassScheduleRequest request);
    void toggleScheduleActive(Integer scheduleId);
    void deleteSchedule(Integer scheduleId);
}
