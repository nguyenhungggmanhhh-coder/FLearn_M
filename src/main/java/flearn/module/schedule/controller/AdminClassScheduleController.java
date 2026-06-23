package flearn.module.schedule.controller;

import flearn.module.management.dto.response.ClassroomResponse;
import flearn.module.management.service.ClassroomService;
import flearn.module.schedule.dto.request.ClassScheduleRequest;
import flearn.module.schedule.dto.response.ClassScheduleResponse;
import flearn.module.schedule.service.ClassScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * AdminClassScheduleController – Admin quản lý lịch học tuần cho từng lớp.
 * URL pattern: /admin/classes/{classId}/schedules
 */
@Controller
@RequestMapping("/admin/classes/{classId}/schedules")
@RequiredArgsConstructor
public class AdminClassScheduleController {

    private final ClassScheduleService classScheduleService;
    private final ClassroomService     classroomService;

    /** Danh sách lịch học của lớp. */
    @GetMapping
    public String list(@PathVariable Integer classId, Model model) {
        ClassroomResponse classroom = classroomService.getClassById(classId);
        List<ClassScheduleResponse> schedules = classScheduleService.getSchedulesByClass(classId);
        model.addAttribute("classroom", classroom);
        model.addAttribute("schedules", schedules);
        model.addAttribute("scheduleRequest", new ClassScheduleRequest());
        // Danh sách thứ để dropdown
        model.addAttribute("daysOfWeek", buildDaysOfWeek());
        return "admin/classes/schedules";
    }

    /** Tạo lịch học mới (POST từ form inline trong trang danh sách). */
    @PostMapping("/create")
    public String create(@PathVariable Integer classId,
                         @Valid @ModelAttribute("scheduleRequest") ClassScheduleRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/admin/classes/" + classId + "/schedules";
        }
        try {
            classScheduleService.createSchedule(classId, request);
            ra.addFlashAttribute("successMsg", "Đã thêm lịch học thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/classes/" + classId + "/schedules";
    }

    /** Sửa lịch học (GET – form edit). */
    @GetMapping("/{scheduleId}/edit")
    public String editForm(@PathVariable Integer classId,
                           @PathVariable Integer scheduleId,
                           Model model) {
        ClassroomResponse classroom = classroomService.getClassById(classId);
        ClassScheduleResponse schedule = classScheduleService.getScheduleById(scheduleId);
        // Map response → request để bind form
        ClassScheduleRequest req = ClassScheduleRequest.builder()
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .roomOrLink(schedule.getRoomOrLink())
                .note(schedule.getNote())
                .remindOneDayBefore(schedule.getRemindOneDayBefore())
                .remindTwoHoursBefore(schedule.getRemindTwoHoursBefore())
                .isActive(schedule.getIsActive())
                .build();
        model.addAttribute("classroom", classroom);
        model.addAttribute("schedule", schedule);
        model.addAttribute("scheduleRequest", req);
        model.addAttribute("daysOfWeek", buildDaysOfWeek());
        return "admin/classes/schedule-edit";
    }

    /** Sửa lịch học (POST). */
    @PostMapping("/{scheduleId}/edit")
    public String update(@PathVariable Integer classId,
                         @PathVariable Integer scheduleId,
                         @Valid @ModelAttribute("scheduleRequest") ClassScheduleRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/admin/classes/" + classId + "/schedules/" + scheduleId + "/edit";
        }
        try {
            classScheduleService.updateSchedule(scheduleId, request);
            ra.addFlashAttribute("successMsg", "Đã cập nhật lịch học.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/classes/" + classId + "/schedules";
    }

    /** Bật/tắt lịch học (không xóa). */
    @PostMapping("/{scheduleId}/toggle")
    public String toggle(@PathVariable Integer classId,
                         @PathVariable Integer scheduleId,
                         RedirectAttributes ra) {
        try {
            classScheduleService.toggleScheduleActive(scheduleId);
            ra.addFlashAttribute("successMsg", "Đã thay đổi trạng thái lịch học.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/classes/" + classId + "/schedules";
    }

    /** Xóa lịch học. */
    @PostMapping("/{scheduleId}/delete")
    public String delete(@PathVariable Integer classId,
                         @PathVariable Integer scheduleId,
                         RedirectAttributes ra) {
        try {
            classScheduleService.deleteSchedule(scheduleId);
            ra.addFlashAttribute("successMsg", "Đã xóa lịch học.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/classes/" + classId + "/schedules";
    }

    // Helper: danh sách thứ cho dropdown
    private java.util.Map<Integer, String> buildDaysOfWeek() {
        java.util.LinkedHashMap<Integer, String> map = new java.util.LinkedHashMap<>();
        map.put(2, "Thứ 2");
        map.put(3, "Thứ 3");
        map.put(4, "Thứ 4");
        map.put(5, "Thứ 5");
        map.put(6, "Thứ 6");
        map.put(7, "Thứ 7");
        map.put(1, "Chủ nhật");
        return map;
    }
}
