package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UserService userService;

    // Hàm này sẽ tự động chạy và đẩy biến "user" vào mọi file HTML
    @ModelAttribute("user")
    public User getLoggedInUser(Authentication authentication) {
        // Kiểm tra xem người dùng đã đăng nhập chưa
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            // Lấy email từ Security và tìm User trong Database
            return userService.findByEmail(authentication.getName());
        }
        return null; // Trả về null nếu chưa đăng nhập (Khách)
    }
}