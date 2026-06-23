package flearn.module.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HomeController - Bộ điều hướng trang chủ và phân luồng điều hướng Dashboard dựa trên vai trò.
 * Đồng thời quản lý các trang trạng thái lỗi hệ thống chung (Access Denied, Bảo trì).
 */
@Controller
public class HomeController {

    /**
     * Endpoint trung gian "/dashboard".
     * Khi đăng nhập thành công, Spring Security sẽ dẫn người dùng đến đây.
     * Tại đây, hệ thống sẽ kiểm tra danh sách Vai trò (Authorities) để redirect về dashboard tương ứng:
     * - ROLE_ADMIN -> /admin/dashboard
     * - ROLE_TEACHER -> /teacher/dashboard
     * - ROLE_STUDENT -> /student/dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        // Lặp qua các quyền (Roles) của người dùng hiện tại
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if (auth.getAuthority().equals("ROLE_ADMIN")) {
                return "redirect:/admin/dashboard"; // Admin về trang Admin
            } else if (auth.getAuthority().equals("ROLE_TEACHER")) {
                return "redirect:/teacher/dashboard"; // Giáo viên về trang Giáo viên
            }
        }
        // Mặc định chuyển hướng về trang học viên
        return "redirect:/student/dashboard";
    }

    /**
     * Hiển thị trang thông báo bảo trì hệ thống.
     * Được kiểm soát và chặn bởi MaintenanceInterceptor khi chế độ bảo trì được kích hoạt.
     */
    @GetMapping("/maintenance")
    public String maintenance() {
        return "error/maintenance";
    }

    /**
     * Hiển thị trang thông báo từ chối truy cập (Access Denied).
     * Xuất hiện khi người dùng truy cập tài nguyên vượt quá thẩm quyền của vai trò hiện tại.
     */
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}
