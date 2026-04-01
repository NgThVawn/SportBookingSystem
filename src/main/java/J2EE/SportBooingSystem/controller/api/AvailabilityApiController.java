package J2EE.SportBooingSystem.controller.api;

import J2EE.SportBooingSystem.dto.response.ApiResponse;
import J2EE.SportBooingSystem.dto.response.AvailabilityResponse;
import J2EE.SportBooingSystem.dto.response.PriceCalculationResponse;
import J2EE.SportBooingSystem.service.BookingService;
import J2EE.SportBooingSystem.service.PriceRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/v1/fields/{fieldId}")
@RequiredArgsConstructor
public class AvailabilityApiController {

    private final BookingService bookingService;
    private final PriceRuleService priceRuleService;

    /**
     * GET /api/v1/fields/{fieldId}/availability?date=2024-12-31
     * Trả về giờ mở cửa, đóng cửa và các khung đã bị chiếm
     */
    @GetMapping("/availability")
    public ApiResponse<AvailabilityResponse> getAvailability(
            @PathVariable Long fieldId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ApiResponse.success(bookingService.getAvailability(fieldId, date));
    }

    /**
     * GET /api/v1/fields/{fieldId}/price?date=2024-12-31&start=08:00&end=10:00
     * Tính giá tạm thời để hiển thị trước khi đặt
     */
    @GetMapping("/price")
    public ApiResponse<PriceCalculationResponse> calculatePrice(
            @PathVariable Long fieldId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime end) {
        return ApiResponse.success(priceRuleService.calculatePrice(fieldId, date, start, end));
    }
}
