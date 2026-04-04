package J2EE.SportBooingSystem.controller;

import J2EE.SportBooingSystem.dto.request.BookingRequest;
import J2EE.SportBooingSystem.dto.response.BookingResponse;
import J2EE.SportBooingSystem.dto.response.PriceCalculationResponse;
import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.entity.Field;
import J2EE.SportBooingSystem.service.BookingService;
import J2EE.SportBooingSystem.service.ExtraServiceService;
import J2EE.SportBooingSystem.service.FieldService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BookingController {

    private final BookingService bookingService;
    private final PriceRuleService priceRuleService;
    private final FieldService fieldService;
    private final ExtraServiceService extraServiceService;

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
                                 @RequestParam(required = false) String start,
                                 @RequestParam(required = false) String end,
                                 Model model) {
        LocalDate bookingDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : null;
        LocalTime startTime = (start != null && !start.isBlank()) ? LocalTime.parse(start) : null;
        LocalTime endTime = (end != null && !end.isBlank()) ? LocalTime.parse(end) : null;
        populateBookingPageModel(model, fieldId, bookingDate, startTime, endTime);
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
            populateBookingPageModel(model, req.getFieldId(), req.getBookingDate(), req.getStartTime(), req.getEndTime());
            return "booking/create";
        }
        try {
            Booking b = bookingService.createBooking(req, ud.getUsername());
            // Redirect tới trang checkout để thanh toán VNPay
            return "redirect:/payment/checkout?bookingCode=" + b.getBookingCode();
        }  catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            populateBookingPageModel(model, req.getFieldId(), req.getBookingDate(), req.getStartTime(), req.getEndTime());
            return "booking/create";
        }
    }

    private void populateBookingPageModel(Model model,
                                          Long fieldId,
                                          LocalDate bookingDate,
                                          LocalTime startTime,
                                          LocalTime endTime) {
        Field field = fieldService.findById(fieldId);
        BigDecimal fieldPrice = BigDecimal.ZERO;
        if (bookingDate != null && startTime != null && endTime != null) {
            fieldPrice = priceRuleService.getTotalPrice(fieldId, bookingDate, startTime, endTime);
        }

        model.addAttribute("fieldId", fieldId);
        model.addAttribute("field", field);
        model.addAttribute("facility", field.getFacility());
        model.addAttribute("date", bookingDate);
        model.addAttribute("start", startTime);
        model.addAttribute("end", endTime);
        model.addAttribute("fieldPrice", fieldPrice);
        model.addAttribute("services", extraServiceService.findByFacility(field.getFacility().getId()));
    }

   /** Hủy booking (User) */
    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id,
                                @RequestParam(required = false, defaultValue = "Khách hàng đổi ý") String reason,
                                @AuthenticationPrincipal UserDetails ud,
                                RedirectAttributes ra) {
        try {
            // Hứng trạng thái trả về từ Service
            J2EE.SportBooingSystem.enums.BookingStatus newStatus = 
                    bookingService.cancelBooking(id, ud.getUsername(), reason);

            // Báo câu thông báo phù hợp
            if (newStatus == J2EE.SportBooingSystem.enums.BookingStatus.CANCEL_PENDING) {
                ra.addFlashAttribute("success", "Do bạn hủy sát giờ (dưới 24h), yêu cầu đã được gửi đến Chủ sân để chờ duyệt!");
            } else {
                ra.addFlashAttribute("success", "Đã hủy đơn đặt sân thành công!");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bookings";
    }
}
