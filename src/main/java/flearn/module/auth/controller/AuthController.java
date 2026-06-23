package flearn.module.auth.controller;

import flearn.module.auth.dto.request.RegisterStudentRequest;
import flearn.module.auth.service.AuthService;
import flearn.common.validation.ValidationMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AuthController - Bộ điều hướng xử lý các yêu cầu Đăng nhập và Đăng ký của người dùng.
 * Phục vụ cho giao diện xác thực chung của hệ thống FLearn.
 */
@Controller
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * Hiển thị trang đăng nhập.
     * Spring Security sẽ tự động chặn các URL cấu hình yêu cầu phân quyền và chuyển hướng về đây.
     */
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    /**
     * Hiển thị trang đăng ký tài khoản cho Student (Học viên mới).
     */
    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    /**
     * Xử lý gửi yêu cầu đăng ký tài khoản từ form đăng ký của Student.
     * 
     * @param request Dữ liệu đăng ký (email, mật khẩu, họ tên...)
     * @param bindingResult Kết quả kiểm tra tính hợp lệ của dữ liệu (validation)
     * @param model Model để truyền thông tin lỗi ra giao diện nếu có
     * @param redirectAttributes Lưu thông báo thành công dạng flash attribute sau khi chuyển hướng
     */
    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute RegisterStudentRequest request,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        // Kiểm tra lỗi hợp lệ dữ liệu đầu vào (ví dụ: email sai định dạng, mật khẩu quá ngắn...)
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMsg", ValidationMessage.firstError(bindingResult));
            return "auth/register";
        }
        try {
            // Thực hiện nghiệp vụ đăng ký tài khoản học viên thông qua AuthService
            authService.registerStudent(request);
            redirectAttributes.addFlashAttribute("successMsg", "Đăng ký thành công. Bạn có thể đăng nhập ngay.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            // Bắt các ngoại lệ logic nghiệp vụ (ví dụ: trùng lặp email...)
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/register";
        }
    }
}
