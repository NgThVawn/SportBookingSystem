package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.dto.request.BookingRequest;
import J2EE.SportBooingSystem.dto.response.BookingResponse;
import J2EE.SportBooingSystem.dto.response.PriceCalculationResponse;
import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.service.BookingService;
import J2EE.SportBooingSystem.service.PriceRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BookingController {

    private final BookingService bookingService;
    private final PriceRuleService priceRuleService;

    /** Trang lịch sử đặt sân của user */
    @GetMapping
    public String myBookings(@AuthenticationPrincipal UserDetails ud, Model model) {
        List<BookingResponse> bookings = bookingService.getBookingsByUser(ud.getUsername());
        model.addAttribute("bookings", bookings);
        return "booking/history";
    }

    /**
     * Trang đặt sân: GET /bookings/new?fieldId=1&date=2024-12-31
     * Hiển thị form chọn giờ + tính giá
     */
    @GetMapping("/new")
    public String newBookingPage(@RequestParam Long fieldId,
                                 @RequestParam String date,
                                 Model model) {
        model.addAttribute("fieldId", fieldId);
        model.addAttribute("date", date);
        model.addAttribute("bookingRequest", new BookingRequest());
        return "booking/create";
    }

    /** Xem giá trước khi xác nhận (AJAX) */
    @PostMapping("/preview-price")
    @ResponseBody
    public PriceCalculationResponse previewPrice(@RequestBody BookingRequest req) {
        return priceRuleService.calculatePrice(
                req.getFieldId(), req.getBookingDate(), req.getStartTime(), req.getEndTime());
    }

    /** Xác nhận đặt sân */
    @PostMapping
    public String createBooking(@Valid @ModelAttribute BookingRequest req,
                                BindingResult br,
                                @AuthenticationPrincipal UserDetails ud,
                                RedirectAttributes ra,
                                Model model) {
        if (br.hasErrors()) {
            model.addAttribute("fieldId", req.getFieldId());
            return "booking/create";
        }
        try {
            Booking b = bookingService.createBooking(req, ud.getUsername());
            ra.addFlashAttribute("success", "Đặt sân thành công! Mã booking: " + b.getBookingCode());
            return "redirect:/bookings";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("fieldId", req.getFieldId());
            return "booking/create";
        }
    }

    /** Hủy booking */
    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id,
                                @RequestParam(required = false) String reason,
                                @AuthenticationPrincipal UserDetails ud,
                                RedirectAttributes ra) {
        try {
            bookingService.cancelBooking(id, ud.getUsername(), reason);
            ra.addFlashAttribute("success", "Đã hủy booking thành công");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bookings";
    }
}
