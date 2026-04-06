package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.entity.Favorite;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.BookingStatus;
import J2EE.SportBooingSystem.repository.BookingRepository;
import J2EE.SportBooingSystem.repository.FavoriteRepository;
import J2EE.SportBooingSystem.security.SecurityUtils;
import J2EE.SportBooingSystem.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/profile") 
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;
    private final FavoriteRepository favoriteRepository;
    private final BookingRepository bookingRepository;

    @GetMapping
    public String profile(
            Model model,
            Authentication authentication,
            @RequestParam(value = "updated", required = false) String updated
    ) {
        if (authentication == null) {
            return "redirect:/auth/login";
        }

        String email = securityUtils.getCurrentUserEmail();
        User user = userService.findByEmail(email);
        model.addAttribute("user", user);

        // THÊM LOGIC TÍNH TOÁN: GIỜ CHƠI VÀ SỐ LẦN ĐẶT SÂN
        long totalBookings = 0;
        long totalMinutes = 0; // Tính tổng số phút trước để không bị làm tròn sai

        try {
            // 1. Lấy toàn bộ lịch sử đặt sân của User
            List<Booking> allBookings = bookingRepository.findByUserOrderByCreatedAtDesc(user);

            // 2. Lọc ra các booking THÀNH CÔNG
            List<Booking> successfulBookings = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .collect(Collectors.toList());

            // 3. Gán tổng số đơn đã đặt thành công
            totalBookings = successfulBookings.size();

            // 4. Tính tổng số phút chơi từ các đơn thành công
            for (Booking b : successfulBookings) {
                if (b.getStartTime() != null && b.getEndTime() != null) {
                    // Dùng java.time.Duration để tính khoảng cách bằng PHÚT
                    long minutes = java.time.Duration.between(b.getStartTime(), b.getEndTime()).toMinutes();
                    totalMinutes += minutes;
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi tính toán thống kê: " + e.getMessage());
        }

        // 5. Quy đổi từ Phút sang Giờ
        double hours = totalMinutes / 60.0;
        
        // Làm đẹp con số: Nếu là 5.0 -> hiển thị "5", nếu là 7.5 -> hiển thị "7.5"
        String displayHours = (hours == (long) hours) 
                              ? String.format("%d", (long) hours) 
                              : String.format("%.1f", hours).replace(",", ".");

        // Đẩy ra View
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("totalHours", displayHours); // Gửi chuỗi giờ đã tính toán chuẩn xác
        // =========================================================

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

        String email = securityUtils.getCurrentUserEmail();
        userService.updateProfile(email, fullName, phone, avatar);

        // Chuyển hướng về lại trang profile kèm thông báo thành công
        return "redirect:/profile?updated=true";
    }

    @GetMapping("/security")
    public String securityPage(Model model, Authentication authentication) {
        User user = userService.findByEmail(securityUtils.getCurrentUserEmail());
        model.addAttribute("user", user);
        return "profile/security"; // Tạo file mới security.html trong thư mục profile
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {

        if (newPassword == null || newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 8 ký tự");
            return "redirect:/profile/security";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
            return "redirect:/profile/security";
        }

        try {
            userService.changePassword(securityUtils.getCurrentUserEmail(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/profile/security";
    }
    @GetMapping("/favorites")
    @Transactional(readOnly = true)
    public String myFavorites(@AuthenticationPrincipal UserDetails ud, Model model) {

        // 1. Lấy thông tin user đang đăng nhập
        User user = userService.findByEmail(ud.getUsername());

        // 2. Lấy danh sách sân từ bảng Yêu thích VÀ ép lấy luôn ảnh
        List<Facility> favFacilities = favoriteRepository.findByUser(user).stream()
                .map(favorite -> {
                    Facility facility = favorite.getFacility();

                    // "Đánh thức" dữ liệu lười biếng (Lazy Load) lúc Session đang mở
                    facility.getPrimaryImageUrl(); // Ép load ảnh
                    facility.getUniqueSportTypes(); // Ép load danh sách môn thể thao (nếu có)

                    return facility;
                })
                .collect(Collectors.toList());

        // 3. Đẩy ra View
        model.addAttribute("facilities", favFacilities);

        // Trỏ đến file HTML: templates/profile/favorites.html
        return "profile/favorites";
    }
    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    public String notificationsPage() {
        return "notifications/list";
    }
}