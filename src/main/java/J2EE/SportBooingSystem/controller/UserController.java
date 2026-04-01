package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile") 
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public String profile(
            Model model,
            Authentication authentication,
            @RequestParam(value = "updated", required = false) String updated
    ) {
        // Kiểm tra đăng nhập (Nếu bạn dùng Spring Security Config tốt thì có thể bỏ qua check null này)
        if (authentication == null) {
            return "redirect:/auth/login";
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email);
        model.addAttribute("user", user);

        // Lưu ý: Biến 'updated' sẽ được Thymeleaf nhận qua param.updated tự động
        // Nhưng nếu bạn muốn dùng biến 'success' như cũ:
        if (updated != null) {
            model.addAttribute("success", true);
        }

        return "profile/profile";
    }

    @PostMapping("/update")
    public String updateProfile(
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam(required = false) MultipartFile avatar,
            Authentication authentication
    ) {
        if (authentication == null) {
            return "redirect:/auth/login";
        }

        String email = authentication.getName();
        userService.updateProfile(email, fullName, phone, avatar);

        // Chuyển hướng về lại trang profile kèm thông báo thành công
        return "redirect:/profile?updated=true";
    }

    @GetMapping("/security")
    public String securityPage(Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        model.addAttribute("user", user);
        return "profile/security"; // Tạo file mới security.html trong thư mục profile
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {


        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
            return "redirect:/profile/security";
        }

        try {
            userService.changePassword(authentication.getName(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/profile/security";
    }
}