package flearn.module.management.controller;

import flearn.module.management.dto.request.AssignTeacherRequest;
import flearn.module.management.dto.request.ClassroomRequest;
import flearn.module.management.dto.response.ClassroomResponse;
import flearn.module.management.dto.response.CourseResponse;
import flearn.module.auth.dto.response.UserResponse;
import flearn.enums.Role;
import flearn.module.management.service.ClassroomService;
import flearn.module.management.service.CourseService;
import flearn.module.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/classes")
@RequiredArgsConstructor
public class AdminClassController {
    private final ClassroomService classroomService;
    private final CourseService courseService;
    private final UserService userService;

    @GetMapping
    public String listClasses(@RequestParam(required = false) String keyword, Model model) {
        List<ClassroomResponse> classes = classroomService.searchAllClasses(keyword);
        model.addAttribute("classes", classes);
        model.addAttribute("keyword", keyword);
        return "admin/classes/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("classroomRequest", new ClassroomRequest());
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("teachers", userService.getUsersByRole(Role.TEACHER));
        return "admin/classes/create";
    }

    @PostMapping("/create")
    public String createClass(@Valid @ModelAttribute ClassroomRequest request,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/admin/classes/create";
        }
        try {
            classroomService.createClass(request);
            redirectAttributes.addFlashAttribute("successMsg", "Da tao lop hoc thanh cong.");
            return "redirect:/admin/classes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/admin/classes/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        ClassroomResponse classroom = classroomService.getClassById(id);
        ClassroomRequest request = ClassroomRequest.builder()
                .className(classroom.getClassName())
                .description(classroom.getDescription())
                .startDate(classroom.getStartDate())
                .endDate(classroom.getEndDate())
                .courseId(classroom.getCourseId())
                .teacherId(classroom.getTeacher() != null ? classroom.getTeacher().getUserId() : null)
                .build();
        model.addAttribute("classroomRequest", request);
        model.addAttribute("classId", id);
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("teachers", userService.getUsersByRole(Role.TEACHER));
        return "admin/classes/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateClass(@PathVariable Integer id,
                              @Valid @ModelAttribute ClassroomRequest request,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/admin/classes/" + id + "/edit";
        }
        try {
            classroomService.updateClass(id, request);
            redirectAttributes.addFlashAttribute("successMsg", "Da cap nhat lop hoc.");
            return "redirect:/admin/classes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/admin/classes/" + id + "/edit";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        ClassroomResponse classroom = classroomService.getClassById(id);
        model.addAttribute("classroom", classroom);
        return "admin/classes/detail";
    }

    @PostMapping("/{id}/assign-teacher")
    public String assignTeacher(@PathVariable Integer id,
                                @Valid @ModelAttribute AssignTeacherRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/admin/classes";
        }
        try {
            classroomService.assignTeacher(id, request);
            redirectAttributes.addFlashAttribute("successMsg", "Da gan teacher cho lop.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    @PostMapping("/{id}/toggle")
    public String toggleStatus(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            classroomService.toggleClassStatus(id);
            redirectAttributes.addFlashAttribute("successMsg", "Da thay doi trang thai lop.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    @PostMapping("/{id}/delete")
    public String deleteClass(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            classroomService.softDeleteClass(id, null);
            redirectAttributes.addFlashAttribute("successMsg", "Da dong lop hoc.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    /** [FEAT-04] Admin toggle mã mời ẩn/hiện kết hợp auto-hide 15 phút. */
    @PostMapping("/{id}/toggle-invite-code")
    public String toggleInviteCode(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            classroomService.toggleInviteCodeVisible(id);
            ra.addFlashAttribute("successMsg", "Dã cập nhật hiển thị mã mời.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/classes/" + id;
    }
}
