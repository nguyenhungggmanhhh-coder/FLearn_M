package flearn.module.auth.controller;

import flearn.module.auth.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * PasswordResetController - Bộ điều hướng xử lý luồng Quên mật khẩu và Đặt lại mật khẩu.
 * Thực hiện quy trình 2 bước xác thực OTP qua email của người dùng.
 */
@Controller
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    // =====================================================
    // BƯỚC 1: Nhập email → kiểm tra và gửi mã OTP qua Email
    // =====================================================

    /**
     * Hiển thị giao diện nhập email để khôi phục mật khẩu.
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    /**
     * Xử lý gửi yêu cầu quên mật khẩu.
     * Kiểm tra sự tồn tại của Email trong cơ sở dữ liệu, nếu hợp lệ sẽ gửi mã OTP đến email đó.
     *
     * @param email Địa chỉ email tài khoản cần khôi phục
     * @param redirectAttributes Dùng để truyền email và thông báo lỗi qua redirect
     */
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                        RedirectAttributes redirectAttributes) {
        try {
            // Thực hiện tạo OTP và gửi qua email cho người dùng
            passwordResetService.sendOtp(email.trim());
            // Lưu lại email tạm thời để điền sẵn vào bước tiếp theo
            redirectAttributes.addFlashAttribute("email", email.trim());
            return "redirect:/reset-password";
        } catch (RuntimeException e) {
            // Trường hợp email không tồn tại hoặc lỗi hệ thống gửi mail
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    // =====================================================
    // BƯỚC 2: Nhập OTP + mật khẩu mới để tiến hành Đặt lại mật khẩu
    // =====================================================

    /**
     * Hiển thị giao diện nhập mã OTP và mật khẩu mới.
     * Ngăn chặn người dùng truy cập trực tiếp URL này khi chưa thực hiện Bước 1 (thiếu email).
     */
    @GetMapping("/reset-password")
    public String showResetPasswordForm(Model model) {
        // Nếu không có thông tin email truyền qua flash attributes (từ Bước 1) -> Quay lại trang nhập email
        if (model.getAttribute("email") == null) {
            return "redirect:/forgot-password";
        }
        return "auth/reset-password";
    }

    /**
     * Xử lý xác thực OTP và cập nhật mật khẩu mới.
     *
     * @param email Email của tài khoản đang cần khôi phục
     * @param otpCode Mã OTP người dùng điền
     * @param newPassword Mật khẩu mới muốn thay đổi
     * @param confirmPassword Xác nhận lại mật khẩu mới
     */
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String email,
                                       @RequestParam String otpCode,
                                       @RequestParam String newPassword,
                                       @RequestParam String confirmPassword,
                                       RedirectAttributes redirectAttributes) {
        // Kiểm tra khớp mật khẩu client-side cơ bản trước khi xử lý
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMsg", "Xác nhận mật khẩu không khớp.");
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/reset-password";
        }
        try {
            // Xác thực mã OTP và đặt lại mật khẩu trong DB qua Service
            passwordResetService.resetPasswordWithOtp(email.trim(), otpCode.trim(), newPassword);
            redirectAttributes.addFlashAttribute("successMsg", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập ngay.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            // Xử lý khi mã OTP sai, hết hạn hoặc tài khoản không tồn tại
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/reset-password";
        }
    }

    // =====================================================
    // /verify-otp — Giữ route cũ chuyển hướng để không bị lỗi 404 nếu người dùng truy cập link cũ
    // =====================================================
    @GetMapping("/verify-otp")
    public String redirectVerifyOtp() {
        return "redirect:/forgot-password";
    }
}
