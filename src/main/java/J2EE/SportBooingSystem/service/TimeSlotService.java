package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.request.TimeSlotGenerateRequest;
import J2EE.SportBooingSystem.dto.response.TimeSlotResponse;
import J2EE.SportBooingSystem.entity.TimeSlot;
import J2EE.SportBooingSystem.enums.SlotStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TimeSlotService {
    List<TimeSlot> generateSlots(TimeSlotGenerateRequest request, String ownerEmail);
    List<TimeSlotResponse> getAvailableSlots(Long fieldId, LocalDate date);
    List<TimeSlotResponse> getAllSlots(Long fieldId, LocalDate date);
    void releaseExpiredLocks();

    void updateStatus(Long slotId, SlotStatus status, String ownerEmail);
    void deleteSlot(Long slotId, String ownerEmail);

    void updateSlotDetail(Long slotId, BigDecimal price, SlotStatus status, String ownerEmail);
}
