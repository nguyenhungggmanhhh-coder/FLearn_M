package flearn.module.content.controller;

import flearn.security.service.CustomUserDetails;
import flearn.module.management.dto.request.ClassroomRequest;
import flearn.module.content.dto.request.LearningLessonRequest;
import flearn.module.content.dto.request.MaterialRequest;
import flearn.module.content.dto.request.RoadmapRequest;
import flearn.module.management.service.ClassroomService;
import flearn.module.management.service.EnrollmentService;
import flearn.module.content.service.LearningContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherClassController {
    private final ClassroomService classroomService;
    private final EnrollmentService enrollmentService;
    private final LearningContentService learningContentService;

    @GetMapping({"", "/dashboard"})
    public String teacherDashboard() {
        return "redirect:/teacher/classes";
    }

    @GetMapping("/classes")
    public String classes(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("fullName", userDetails.getUser().getFullName());
        model.addAttribute("classes", classroomService.getClassesByTeacher(userDetails.getUser()));
        return "teacher/classes/list";
    }

    @GetMapping("/classes/{id}")
    public String detail(@PathVariable Integer id,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         @RequestParam(required = false) String keyword,
                         Model model) {
        model.addAttribute("classroom", classroomService.getTeacherClassById(id, userDetails.getUser()));
        model.addAttribute("students", enrollmentService.searchActiveStudentsInClass(id, userDetails.getUser(), keyword));
        model.addAttribute("keyword", keyword);
        return "teacher/classes/detail";
    }

    @PostMapping("/classes/{id}/toggle")
    public String toggle(@PathVariable Integer id,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            classroomService.toggleTeacherClassStatus(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMsg", "Da doi trang thai lop.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/classes";
    }

    @PostMapping("/classes/{id}/toggle-joinable")
    public String toggleJoinable(@PathVariable Integer id,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes,
                                 jakarta.servlet.http.HttpServletRequest request) {
        try {
            classroomService.toggleJoinable(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMsg", "Đã thay đổi trạng thái khóa/mở mã tham gia lớp.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/teacher/classes/" + id;
    }

    @PostMapping("/classes/{id}/delete")
    public String delete(@PathVariable Integer id,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            classroomService.softDeleteClass(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMsg", "Da dong lop hoc.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/classes";
    }

    // ==========================================
    // MODULE 3: Roadmap management
    // ==========================================

    @GetMapping("/classes/{classId}/roadmaps")
    public String roadmaps(@PathVariable Integer classId,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           Model model) {
        model.addAttribute("classroom", classroomService.getTeacherClassById(classId, userDetails.getUser()));
        model.addAttribute("roadmaps", learningContentService.getTeacherRoadmaps(classId, userDetails.getUser()));
        return "teacher/classes/roadmaps";
    }

    @GetMapping("/classes/{classId}/roadmaps/create")
    public String createRoadmapForm(@PathVariable Integer classId,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    Model model) {
        model.addAttribute("classroom", classroomService.getTeacherClassById(classId, userDetails.getUser()));
        model.addAttribute("roadmapRequest", new RoadmapRequest());
        model.addAttribute("formAction", "/teacher/classes/" + classId + "/roadmaps/create");
        model.addAttribute("pageTitle", "Tao roadmap");
        return "teacher/classes/roadmap-form";
    }

    @PostMapping("/classes/{classId}/roadmaps/create")
    public String createRoadmap(@PathVariable Integer classId,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                @Valid @ModelAttribute RoadmapRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/teacher/classes/" + classId + "/roadmaps/create";
        }
        try {
            learningContentService.createRoadmap(classId, userDetails.getUser(), request);
            redirectAttributes.addFlashAttribute("successMsg", "Da tao roadmap.");
            return "redirect:/teacher/classes/" + classId + "/roadmaps";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/teacher/classes/" + classId + "/roadmaps/create";
        }
    }

    @GetMapping("/roadmaps/{id}/edit")
    public String editRoadmapForm(@PathVariable Integer id,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        var roadmap = learningContentService.getTeacherRoadmapWithContent(id, userDetails.getUser());
        model.addAttribute("roadmap", roadmap);
        model.addAttribute("roadmapRequest", RoadmapRequest.builder()
                .title(roadmap.getTitle())
                .description(roadmap.getDescription())
                .build());
        model.addAttribute("lessonRequest", LearningLessonRequest.builder().orderIndex(0).build());
        model.addAttribute("formAction", "/teacher/roadmaps/" + id + "/edit");
        model.addAttribute("pageTitle", "Cap nhat roadmap");
        return "teacher/classes/roadmap-form";
    }

    @PostMapping("/roadmaps/{id}/edit")
    public String updateRoadmap(@PathVariable Integer id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                @Valid @ModelAttribute RoadmapRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        var roadmap = learningContentService.getTeacherRoadmap(id, userDetails.getUser());
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/teacher/roadmaps/" + id + "/edit";
        }
        try {
            learningContentService.updateRoadmap(id, userDetails.getUser(), request);
            redirectAttributes.addFlashAttribute("successMsg", "Da cap nhat roadmap.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/classes/" + roadmap.getClassRoom().getClassId() + "/roadmaps";
    }

    @PostMapping("/roadmaps/{id}/delete")
    public String deleteRoadmap(@PathVariable Integer id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        var roadmap = learningContentService.getTeacherRoadmap(id, userDetails.getUser());
        Integer classId = roadmap.getClassRoom().getClassId();
        try {
            learningContentService.deleteRoadmap(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMsg", "Da xoa roadmap.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/classes/" + classId + "/roadmaps";
    }

    @PostMapping("/roadmaps/{id}/publish")
    public String publishRoadmap(@PathVariable Integer id,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        var roadmap = learningContentService.getTeacherRoadmap(id, userDetails.getUser());
        try {
            learningContentService.toggleRoadmapPublished(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMsg", "Da doi trang thai publish roadmap.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/classes/" + roadmap.getClassRoom().getClassId() + "/roadmaps";
    }

    // ==========================================
    // MODULE 3: Lesson management
    // ==========================================

    @GetMapping("/roadmaps/{id}/lessons/create")
    public String createLessonForm(@PathVariable Integer id,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        model.addAttribute("roadmap", learningContentService.getTeacherRoadmap(id, userDetails.getUser()));
        model.addAttribute("lessonRequest", LearningLessonRequest.builder().orderIndex(0).build());
        model.addAttribute("formAction", "/teacher/roadmaps/" + id + "/lessons/create");
        model.addAttribute("pageTitle", "Tao lesson");
        return "teacher/classes/lesson-form";
    }

    @PostMapping("/roadmaps/{id}/lessons/create")
    public String createLesson(@PathVariable Integer id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               @Valid @ModelAttribute LearningLessonRequest request,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/teacher/roadmaps/" + id + "/lessons/create";
        }
        try {
            learningContentService.createLesson(id, userDetails.getUser(), request);
            redirectAttributes.addFlashAttribute("successMsg", "Da tao lesson.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/roadmaps/" + id + "/edit";
    }

    @GetMapping("/lessons/{id}/edit")
    public String editLessonForm(@PathVariable Integer id,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
        var lesson = learningContentService.getTeacherLesson(id, userDetails.getUser());
        model.addAttribute("lesson", lesson);
        model.addAttribute("materials", learningContentService.getTeacherLessonMaterials(id, userDetails.getUser()));
        model.addAttribute("materialRequest", new MaterialRequest());
        model.addAttribute("materialTypes", learningContentService.getMaterialTypes());
        model.addAttribute("lessonRequest", LearningLessonRequest.builder()
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .orderIndex(lesson.getOrderIndex())
                .videoUrl(lesson.getVideoUrl())
                .build());
        model.addAttribute("formAction", "/teacher/lessons/" + id + "/edit");
        model.addAttribute("pageTitle", "Cap nhat lesson");
        return "teacher/classes/lesson-form";
    }

    @PostMapping("/lessons/{id}/edit")
    public String updateLesson(@PathVariable Integer id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               @Valid @ModelAttribute LearningLessonRequest request,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        var lesson = learningContentService.getTeacherLesson(id, userDetails.getUser());
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/teacher/lessons/" + id + "/edit";
        }
        try {
            learningContentService.updateLesson(id, userDetails.getUser(), request);
            redirectAttributes.addFlashAttribute("successMsg", "Da cap nhat lesson.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/roadmaps/" + lesson.getRoadmapId() + "/edit";
    }

    @PostMapping("/lessons/{id}/toggle")
    public String toggleLesson(@PathVariable Integer id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        var lesson = learningContentService.getTeacherLesson(id, userDetails.getUser());
        try {
            learningContentService.toggleLessonVisible(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMsg", "Da doi trang thai lesson.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/roadmaps/" + lesson.getRoadmapId() + "/edit";
    }

    @PostMapping("/lessons/{id}/delete")
    public String deleteLesson(@PathVariable Integer id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        var lesson = learningContentService.getTeacherLesson(id, userDetails.getUser());
        Integer roadmapId = lesson.getRoadmapId();
        try {
            learningContentService.deleteLesson(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMsg", "Da xoa lesson.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/roadmaps/" + roadmapId + "/edit";
    }

    // ==========================================
    // MODULE 3: Material management
    // ==========================================

    @GetMapping("/lessons/{id}/materials/create")
    public String createMaterialForm(@PathVariable Integer id,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     Model model) {
        model.addAttribute("lesson", learningContentService.getTeacherLesson(id, userDetails.getUser()));
        model.addAttribute("materialRequest", new MaterialRequest());
        model.addAttribute("materialTypes", learningContentService.getMaterialTypes());
        model.addAttribute("formAction", "/teacher/lessons/" + id + "/materials/create");
        model.addAttribute("pageTitle", "Tao material");
        return "teacher/classes/material-form";
    }

    @PostMapping("/lessons/{id}/materials/create")
    public String createMaterial(@PathVariable Integer id,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 @Valid @ModelAttribute MaterialRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/teacher/lessons/" + id + "/materials/create";
        }
        try {
            learningContentService.createMaterial(id, userDetails.getUser(), request);
            redirectAttributes.addFlashAttribute("successMsg", "Da tao material.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/lessons/" + id + "/edit";
    }

    @GetMapping("/materials/{id}/edit")
    public String editMaterialForm(@PathVariable Integer id,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        var material = learningContentService.getTeacherMaterial(id, userDetails.getUser());
        model.addAttribute("material", material);
        model.addAttribute("materialRequest", MaterialRequest.builder()
                .title(material.getTitle())
                .description(material.getDescription())
                .type(material.getType())
                .externalUrl(material.getExternalUrl())
                .build());
        model.addAttribute("materialTypes", learningContentService.getMaterialTypes());
        model.addAttribute("formAction", "/teacher/materials/" + id + "/edit");
        model.addAttribute("pageTitle", "Cap nhat material");
        return "teacher/classes/material-form";
    }

    @PostMapping("/materials/{id}/edit")
    public String updateMaterial(@PathVariable Integer id,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 @Valid @ModelAttribute MaterialRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        var material = learningContentService.getTeacherMaterial(id, userDetails.getUser());
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/teacher/materials/" + id + "/edit";
        }
        try {
            learningContentService.updateMaterial(id, userDetails.getUser(), request);
            redirectAttributes.addFlashAttribute("successMsg", "Da cap nhat material.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/lessons/" + material.getLessonId() + "/edit";
    }

    @PostMapping("/materials/{id}/publish")
    public String publishMaterial(@PathVariable Integer id,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        var material = learningContentService.getTeacherMaterial(id, userDetails.getUser());
        try {
            learningContentService.toggleMaterialPublished(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMsg", "Da doi trang thai publish material.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/lessons/" + material.getLessonId() + "/edit";
    }

    @PostMapping("/materials/{id}/delete")
    public String deleteMaterial(@PathVariable Integer id,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        var material = learningContentService.getTeacherMaterial(id, userDetails.getUser());
        Integer lessonId = material.getLessonId();
        try {
            learningContentService.deleteMaterial(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMsg", "Da xoa material.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/teacher/lessons/" + lessonId + "/edit";
    }
}
