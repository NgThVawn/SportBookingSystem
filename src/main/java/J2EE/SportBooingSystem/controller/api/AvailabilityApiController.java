package J2EE.SportBooingSystem.controller.api;

import J2EE.SportBooingSystem.dto.response.ApiResponse;
import J2EE.SportBooingSystem.dto.response.AvailabilityResponse;
import J2EE.SportBooingSystem.dto.response.ExtraServiceOptionResponse;
import J2EE.SportBooingSystem.dto.response.PriceCalculationResponse;
import J2EE.SportBooingSystem.service.ExtraServiceService;
import J2EE.SportBooingSystem.service.BookingService;
import J2EE.SportBooingSystem.service.PriceRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fields/{fieldId}")
@RequiredArgsConstructor
public class AvailabilityApiController {

    private final BookingService bookingService;
    private final PriceRuleService priceRuleService;
    private final ExtraServiceService extraServiceService;

  
    @GetMapping("/availability")
    public ApiResponse<AvailabilityResponse> getAvailability(
            @PathVariable Long fieldId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ApiResponse.success(bookingService.getAvailability(fieldId, date));
    }

   
    @GetMapping("/price")
    public ApiResponse<PriceCalculationResponse> calculatePrice(
            @PathVariable Long fieldId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime end) {
        return ApiResponse.success(priceRuleService.calculatePrice(fieldId, date, start, end));
    }

    @GetMapping("/services")
    public ApiResponse<List<ExtraServiceOptionResponse>> getServicesForField(@PathVariable Long fieldId) {
        var result = extraServiceService.findByField(fieldId).stream()
                .map(ExtraServiceOptionResponse::from)
                .toList();
        return ApiResponse.success(result);
    }
}
