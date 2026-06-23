package flearn.module.auth.controller;

import flearn.security.service.CustomUserDetails;
import flearn.module.auth.dto.request.ChangePasswordRequest;
import flearn.module.auth.service.AuthService;
import flearn.common.validation.ValidationMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * AccountController - Bộ điều hướng xử lý thông tin cá nhân và tài khoản của người dùng.
 * Phụ trách các tính năng sau khi đăng nhập thành công như Đổi mật khẩu.
 */
@Controller
@RequiredArgsConstructor
public class AccountController {
    private final AuthService authService;

    /**
     * Hiển thị trang đổi mật khẩu (Change Password).
     * Yêu cầu người dùng phải đăng nhập trước (được quản lý bởi SecurityConfig).
     */
    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "auth/change-password";
    }

    /**
     * Xử lý yêu cầu đổi mật khẩu sau khi gửi form.
     * 
     * @param request Dữ liệu chứa mật khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu
     * @param bindingResult Chứa kết quả kiểm tra tính hợp lệ dữ liệu (validation)
     * @param userDetails Đối tượng chứa thông tin người dùng đang đăng nhập lấy từ Spring Security Context
     * @param model Model phục vụ việc truyền các thông báo thành công hoặc lỗi ra ngoài view
     */
    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute ChangePasswordRequest request,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
        // Kiểm tra tính hợp lệ dữ liệu đầu vào (ví dụ: các ô không được để trống, độ dài tối thiểu...)
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMsg", ValidationMessage.firstError(bindingResult));
            return "auth/change-password";
        }

        try {
            // Gọi AuthService thực hiện cập nhật mật khẩu mới cho user hiện tại
            authService.changePassword(userDetails.getUser(), request);
            model.addAttribute("successMsg", "Đổi mật khẩu thành công.");
        } catch (RuntimeException exception) {
            // Bắt các ngoại lệ nghiệp vụ (ví dụ: mật khẩu hiện tại nhập sai, xác nhận mật khẩu mới không khớp...)
            model.addAttribute("errorMsg", exception.getMessage());
        }
        return "auth/change-password";
    }
}
