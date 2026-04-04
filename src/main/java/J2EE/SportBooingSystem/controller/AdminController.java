package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.enums.FacilityStatus;
import J2EE.SportBooingSystem.entity.Facility;
import J2EE.SportBooingSystem.entity.User;
import J2EE.SportBooingSystem.enums.NotificationType;
import J2EE.SportBooingSystem.repository.FacilityRepository;
import J2EE.SportBooingSystem.service.FacilityService;
import J2EE.SportBooingSystem.service.NotificationService;
import J2EE.SportBooingSystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final FacilityService facilityService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final FacilityRepository facilityRepository;

    @GetMapping("")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails ud) {
        User currentUser = userService.findByEmail(ud.getUsername());
        List<User> users = userService.findAll();
        List<Facility> facilities = facilityService.findAll();
        List<Facility> recentFacilities = facilityService.findAllForAdmin(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        List<User> recentUsers = users.stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .collect(Collectors.toList());

        long bannedUsers = users.stream().filter(user -> Boolean.TRUE.equals(user.getIsBanned())).count();
        long pendingFacilities = facilities.stream()
                .filter(facility -> facility.getStatus() == FacilityStatus.PENDING_APPROVAL)
                .count();
        long activeFacilities = facilities.stream()
                .filter(facility -> facility.getStatus() == FacilityStatus.OPEN)
                .count();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("bannedUsers", bannedUsers);
        model.addAttribute("totalFacilities", facilityService.count());
        model.addAttribute("activeFacilities", activeFacilities);
        model.addAttribute("pendingFacilities", pendingFacilities);
        model.addAttribute("recentFacilities", recentFacilities);
        model.addAttribute("recentUsers", recentUsers);

        return "admin/index";
    }

    @GetMapping("/facilities")
    public String manageFacilities(@RequestParam(defaultValue = "0") int page, Model model) {
        var facilities = facilityService.findAllForAdmin(
            PageRequest.of(page, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        model.addAttribute("facilities", facilities);

        model.addAttribute("FacilityStatus", FacilityStatus.values()); 
        
        return "admin/facilities"; 
    }

    @PostMapping("/facilities/{id}/status")
    public String changeFacilityStatus(@PathVariable Long id,
                                       @RequestParam String status,
                                       RedirectAttributes ra) {
        try {
            FacilityStatus newStatus = FacilityStatus.valueOf(status.toUpperCase());
            facilityService.changeStatusByAdmin(id, newStatus);
            facilityRepository.findById(id).ifPresent(facility -> {
                if (newStatus == FacilityStatus.OPEN) {
                    notificationService.send(
                            facility.getOwner(),
                            NotificationType.FACILITY_APPROVED,
                            "Cơ sở đã được duyệt",
                            "Cơ sở \"" + facility.getName() + "\" của bạn đã được Admin phê duyệt và đang hoạt động.",
                            "/owner/facilities"
                    );
                } else if (newStatus == FacilityStatus.BLOCKED) {
                    notificationService.send(
                            facility.getOwner(),
                            NotificationType.FACILITY_REJECTED,
                            "Cơ sở bị khóa",
                            "Cơ sở \"" + facility.getName()
                                    + "\" của bạn đã bị Admin khóa. Liên hệ hỗ trợ để biết thêm chi tiết.",
                            "/owner/facilities"
                    );
                }
            });
            ra.addFlashAttribute("successMsg", "Đã cập nhật trạng thái cơ sở thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/facilities";
    }
    
    @GetMapping("/users")
    public String manageUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users"; 
    }

    @PostMapping("/users/{id}/ban")
    public String banUser(@PathVariable Long id, 
                          @RequestParam String reason, 
                          @AuthenticationPrincipal UserDetails ud, // Lấy người đang thao tác
                          RedirectAttributes ra) {
        try {

            userService.banUser(id, reason, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Đã khóa tài khoản người dùng!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/unban")
    public String unbanUser(@PathVariable Long id, 
                            @AuthenticationPrincipal UserDetails ud, // Lấy người đang thao tác
                            RedirectAttributes ra) {
        try {
            userService.unbanUser(id, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Đã mở khóa tài khoản!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/promote")
    public String promoteToAdmin(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails ud, // <-- 1. Thêm cái này để lấy thông tin người đang đăng nhập
                                 RedirectAttributes ra) {
        try {
            // 2. Truyền thêm ud.getUsername() (là email) vào hàm này
            userService.promoteToAdmin(id, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Đã cấp quyền ADMIN cho người dùng!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/demote")
    public String demoteFromAdmin(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails ud,
                                  RedirectAttributes ra) {
        try {
            
            userService.demoteFromAdmin(id, ud.getUsername());
            ra.addFlashAttribute("successMsg", "Đã thu hồi quyền ADMIN của người dùng!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage()); 
        }
        return "redirect:/admin/users";
    }
    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()") // Hoặc hasRole('ADMIN')
    public String adminNotificationsPage(Model model) {
        
        // Gắn cờ báo hiệu đây là giao diện của Admin
        model.addAttribute("isAdmin", true); 
        
        return "notifications/list"; 
    }
}