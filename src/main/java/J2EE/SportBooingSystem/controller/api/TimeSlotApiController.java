package J2EE.SportBooingSystem.controller.api;

import J2EE.SportBooingSystem.dto.response.ApiResponse;
import J2EE.SportBooingSystem.dto.response.TimeSlotResponse;
import J2EE.SportBooingSystem.enums.SlotStatus;
import J2EE.SportBooingSystem.service.TimeSlotService;
import J2EE.SportBooingSystem.service.FieldService;
import J2EE.SportBooingSystem.entity.Field;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/facilities/{facilityId}/fields/{fieldId}")
@RequiredArgsConstructor
public class TimeSlotApiController {

    private final TimeSlotService timeSlotService;
    private final FieldService fieldService;

    @GetMapping("/slots")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getSlots(
            @PathVariable Long facilityId,
            @PathVariable Long fieldId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        validateFieldInFacility(facilityId, fieldId);
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(timeSlotService.getAvailableSlots(fieldId, queryDate)));
    }

    @GetMapping("/all-slots")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getAllSlots(
            @PathVariable Long facilityId,
            @PathVariable Long fieldId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        validateFieldInFacility(facilityId, fieldId);
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(timeSlotService.getAllSlots(fieldId, queryDate)));
    }

    // --- MỚI: Cập nhật nhanh giá và trạng thái cho từng Slot ---
    @PatchMapping("/slots/{slotId}")
    public ResponseEntity<ApiResponse<Void>> updateSlot(
            @PathVariable Long facilityId,
            @PathVariable Long fieldId,
            @PathVariable Long slotId,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) SlotStatus status,
            @AuthenticationPrincipal UserDetails ud) {
        
        validateFieldInFacility(facilityId, fieldId);
        timeSlotService.updateSlotDetail(slotId, price, status, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private void validateFieldInFacility(Long facilityId, Long fieldId) {
        Field field = fieldService.findById(fieldId);
        if (!field.getFacility().getId().equals(facilityId)) {
            throw new IllegalArgumentException("Sân bãi không thuộc về cơ sở này!");
        }
    }
}