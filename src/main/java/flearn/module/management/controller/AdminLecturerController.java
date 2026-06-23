package flearn.module.management.controller;

import flearn.module.management.dto.request.CreateTeacherRequest;
import flearn.module.auth.dto.request.UpdateUserRequest;
import flearn.enums.Role;
import flearn.module.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/lecturers")
@RequiredArgsConstructor
public class AdminLecturerController {
    private final UserService userService;

    @GetMapping
    public String lecturers(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("teachers", userService.searchUsersByRole(Role.TEACHER, keyword));
        model.addAttribute("keyword", keyword);
        return "admin/lecturers/list";
    }

    @GetMapping("/create")
    public String createTeacherForm(Model model) {
        model.addAttribute("createTeacherRequest", new CreateTeacherRequest());
        return "admin/lecturers/create";
    }

    @PostMapping("/create")
    public String createTeacher(@Valid @ModelAttribute CreateTeacherRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/admin/lecturers/create";
        }
        try {
            userService.createTeacher(request);
            redirectAttributes.addFlashAttribute("successMsg", "Da tao tai khoan teacher thanh cong.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/admin/lecturers/create";
        }
        return "redirect:/admin/lecturers";
    }

    @PostMapping("/{id}/update")
    public String updateTeacher(@PathVariable Integer id,
                                @Valid @ModelAttribute UpdateUserRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/admin/lecturers";
        }
        try {
            userService.updateUser(id, request);
            redirectAttributes.addFlashAttribute("successMsg", "Da cap nhat teacher.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/lecturers";
    }

    @PostMapping("/{id}/block")
    public String blockTeacher(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            userService.blockUser(id);
            redirectAttributes.addFlashAttribute("successMsg", "Da block teacher.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/lecturers";
    }

    @PostMapping("/{id}/unblock")
    public String unblockTeacher(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            userService.unblockUser(id);
            redirectAttributes.addFlashAttribute("successMsg", "Da unblock teacher.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/lecturers";
    }
}
