package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.dto.request.ReviewRequest;
import J2EE.SportBooingSystem.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public String submitReview(@ModelAttribute ReviewRequest request,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra đăng nhập: Nếu chưa đăng nhập, đẩy về trang login
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            // 2. Gọi tầng Service để xử lý lưu review và cập nhật avgRating
            reviewService.createReview(request, userDetails.getUsername());

            // 3. Nếu thành công, gửi thông báo (Flash Message) xuống giao diện
            redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã gửi đánh giá! Điểm số của sân đã được cập nhật.");

        } catch (IllegalStateException | IllegalArgumentException e) {
            // 4. Bắt các lỗi logic từ Service (VD: Chưa từng đặt sân, Đã đánh giá rồi...)
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            // Bắt lỗi hệ thống
            redirectAttributes.addFlashAttribute("errorMessage", "Đã có lỗi xảy ra. Vui lòng thử lại sau.");
        }

        // 5. Quay trở lại trang chi tiết của đúng cơ sở mà người dùng vừa đánh giá
        return "redirect:/bookings";
    }
}
