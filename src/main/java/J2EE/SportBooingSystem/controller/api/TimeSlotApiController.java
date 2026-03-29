package J2EE.SportBooingSystem.controller.api;

import J2EE.SportBooingSystem.dto.response.ApiResponse;
import J2EE.SportBooingSystem.dto.response.TimeSlotResponse;
import J2EE.SportBooingSystem.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TimeSlotApiController {

    private final TimeSlotService timeSlotService;

    @GetMapping("/fields/{fieldId}/slots")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getSlots(
            @PathVariable Long fieldId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<TimeSlotResponse> slots = timeSlotService.getAvailableSlots(fieldId, date);
        return ResponseEntity.ok(ApiResponse.ok(slots));
    }

    @GetMapping("/fields/{fieldId}/all-slots")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getAllSlots(
            @PathVariable Long fieldId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<TimeSlotResponse> slots = timeSlotService.getAllSlots(fieldId, date);
        return ResponseEntity.ok(ApiResponse.ok(slots));
    }
}
