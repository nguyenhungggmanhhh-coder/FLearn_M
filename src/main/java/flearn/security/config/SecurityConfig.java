package flearn.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig - Cấu hình bảo mật trung tâm của hệ thống sử dụng Spring Security.
 * Định nghĩa phân quyền truy cập endpoint, cách thức mã hóa mật khẩu, cấu hình đăng nhập/đăng xuất và cookie "Remember Me".
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Khai báo Bean PasswordEncoder sử dụng thuật toán BCrypt mã hóa mật khẩu.
     * Dùng để băm mật khẩu khi đăng ký và so sánh mật khẩu khi đăng nhập.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cấu hình chuỗi bộ lọc bảo mật (Security Filter Chain).
     * Định nghĩa quy tắc kiểm soát truy cập cho toàn bộ các URL trong ứng dụng.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Phân quyền các Request (Yêu cầu HTTP)
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập công khai không cần đăng nhập vào tài nguyên tĩnh và các trang xác thực cơ bản
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/maintenance", "/register", "/login",
                                "/forgot-password", "/verify-otp", "/reset-password", "/access-denied").permitAll()
                        // Phân quyền theo vai trò người dùng (Role-based Authorization)
                        .requestMatchers("/admin/**").hasRole("ADMIN")      // Các URL bắt đầu bằng /admin/ yêu cầu quyền ADMIN
                        .requestMatchers("/teacher/**").hasRole("TEACHER")  // Các URL bắt đầu bằng /teacher/ yêu cầu quyền TEACHER
                        .requestMatchers("/student/**").hasRole("STUDENT")  // Các URL bắt đầu bằng /student/ yêu cầu quyền STUDENT
                        // Các trang dùng chung yêu cầu người dùng phải xác thực (đăng nhập) trước
                        .requestMatchers("/change-password", "/dashboard", "/logout").authenticated()
                        // Mọi yêu cầu khác đều phải đăng nhập
                        .anyRequest().authenticated()
                )
                // 2. Cấu hình Form Login (Giao diện đăng nhập mặc định của hệ thống)
                .formLogin(form -> form
                        .loginPage("/login")                    // Đường dẫn đến trang đăng nhập tùy chỉnh
                        .defaultSuccessUrl("/dashboard", true) // Đường dẫn mặc định sau khi đăng nhập thành công (redirect về HomeController để phân luồng)
                        .failureUrl("/login?error=true")       // Trang redirect khi đăng nhập thất bại
                        .permitAll()
                )
                // 3. Cấu hình Đăng xuất (Logout)
                .logout(logout -> logout
                        .logoutUrl("/logout")                       // Đường dẫn kích hoạt đăng xuất (POST)
                        .logoutSuccessUrl("/login?logout=true")     // Đường dẫn chuyển hướng sau khi đăng xuất thành công
                        .invalidateHttpSession(true)                // [FIX BUG-01/02] Xóa toàn bộ session khi đăng xuất
                        .deleteCookies("JSESSIONID", "remember-me") // [FIX BUG-01/02] Xóa cookie session và remember-me
                        .clearAuthentication(true)                  // [FIX BUG-01/02] Xóa thông tin xác thực khỏi SecurityContext
                        .permitAll()
                )
                // 4. Xử lý ngoại lệ phân quyền
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/access-denied")    // Điều hướng đến trang thông báo từ chối truy cập khi lỗi 403
                )
                // 5. Cấu hình tính năng Remember Me (Duy trì đăng nhập qua Cookie)
                .rememberMe(remember -> remember
                        .key("BiMat")                          // Khóa bí mật dùng để mã hóa Cookie duy trì đăng nhập
                        .tokenValiditySeconds(7 * 24 * 60 * 60)// Thời gian duy trì: 7 ngày (tính bằng giây)
                )
                // 6. [FIX BUG-02] Quản lý Session – ngăn Session Fixation Attack và layout lỗi sau đổi role
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()     // Tạo session ID mới khi đăng nhập, giữ attributes – ngăn session cũ của role trước bị reuse
                        .maximumSessions(1)                     // Giới hạn 1 session active mỗi tài khoản
                );

        return http.build();
    }
}
