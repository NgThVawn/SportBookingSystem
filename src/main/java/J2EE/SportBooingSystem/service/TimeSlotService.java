package J2EE.SportBooingSystem.service;

import J2EE.SportBooingSystem.dto.request.TimeSlotGenerateRequest;
import J2EE.SportBooingSystem.dto.response.TimeSlotResponse;
import J2EE.SportBooingSystem.entity.TimeSlot;

import java.time.LocalDate;
import java.util.List;

public interface TimeSlotService {
    List<TimeSlot> generateSlots(TimeSlotGenerateRequest request, String ownerEmail);
    List<TimeSlotResponse> getAvailableSlots(Long fieldId, LocalDate date);
    List<TimeSlotResponse> getAllSlots(Long fieldId, LocalDate date);
    void releaseExpiredLocks();
}
