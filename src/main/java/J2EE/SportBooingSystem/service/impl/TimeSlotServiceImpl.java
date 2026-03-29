package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.request.TimeSlotGenerateRequest;
import J2EE.SportBooingSystem.dto.response.TimeSlotResponse;
import J2EE.SportBooingSystem.entity.Field;
import J2EE.SportBooingSystem.entity.TimeSlot;
import J2EE.SportBooingSystem.enums.SlotStatus;
import J2EE.SportBooingSystem.repository.TimeSlotRepository;
import J2EE.SportBooingSystem.service.FieldService;
import J2EE.SportBooingSystem.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TimeSlotServiceImpl implements TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final FieldService fieldService;

    @Override
    public List<TimeSlot> generateSlots(TimeSlotGenerateRequest req, String ownerEmail) {
        Field field = fieldService.findById(req.getFieldId());
        if (!field.getFacility().getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Not authorized");
        }

        List<TimeSlot> created = new ArrayList<>();
        LocalDate date = req.getStartDate();

        while (!date.isAfter(req.getEndDate())) {
            LocalTime current = req.getDayStartTime();
            while (current.plusMinutes(field.getSlotDuration()).compareTo(req.getDayEndTime()) <= 0) {
                LocalTime end = current.plusMinutes(field.getSlotDuration());
                // Skip if slot already exists
                if (!timeSlotRepository.existsByFieldAndDateAndStartTime(field, date, current)) {
                    TimeSlot slot = TimeSlot.builder()
                        .field(field)
                        .date(date)
                        .startTime(current)
                        .endTime(end)
                        .status(SlotStatus.AVAILABLE)
                        .build();
                    created.add(timeSlotRepository.save(slot));
                }
                current = end;
            }
            date = date.plusDays(1);
        }
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableSlots(Long fieldId, LocalDate date) {
        Field field = fieldService.findById(fieldId);
        return timeSlotRepository
            .findByFieldAndDateAndStatusOrderByStartTime(field, date, SlotStatus.AVAILABLE)
            .stream()
            .map(TimeSlotResponse::from)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAllSlots(Long fieldId, LocalDate date) {
        Field field = fieldService.findById(fieldId);
        return timeSlotRepository
            .findByFieldAndDateOrderByStartTime(field, date)
            .stream()
            .map(TimeSlotResponse::from)
            .collect(Collectors.toList());
    }

    @Override
    public void releaseExpiredLocks() {
        timeSlotRepository.releaseExpiredLocks(LocalDateTime.now());
    }
}
