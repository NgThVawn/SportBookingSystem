package J2EE.SportBooingSystem.service.impl;

import J2EE.SportBooingSystem.dto.request.TimeSlotGenerateRequest;
import J2EE.SportBooingSystem.dto.response.TimeSlotResponse;
import J2EE.SportBooingSystem.entity.Field;
import J2EE.SportBooingSystem.entity.TimeSlot;
import J2EE.SportBooingSystem.enums.SlotStatus;
import J2EE.SportBooingSystem.exception.ResourceNotFoundException;
import J2EE.SportBooingSystem.repository.TimeSlotRepository;
import J2EE.SportBooingSystem.service.FieldService;
import J2EE.SportBooingSystem.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        
        // 1. Kiểm tra quyền sở hữu trước khi tạo
        if (!field.getFacility().getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Bạn không có quyền tạo lịch cho sân này!");
        }

        List<TimeSlot> created = new ArrayList<>();
        LocalDate date = req.getStartDate();

        while (!date.isAfter(req.getEndDate())) {
            LocalTime current = req.getDayStartTime();
            while (current.plusMinutes(field.getSlotDuration()).compareTo(req.getDayEndTime()) <= 0) {
                LocalTime end = current.plusMinutes(field.getSlotDuration());
                
                if (!timeSlotRepository.existsByFieldAndDateAndStartTime(field, date, current)) {
                    TimeSlot slot = TimeSlot.builder()
                        .field(field)
                        .date(date)
                        .startTime(current)
                        .endTime(end)
                        .status(SlotStatus.AVAILABLE)
                        .priceOverride(req.getPriceOverride()) 
                        .build();
                    created.add(slot);
                }
                current = end;
            }
            date = date.plusDays(1);
        }

        return timeSlotRepository.saveAll(created);
    }


    @Override
    public void updateSlotDetail(Long slotId, BigDecimal price, SlotStatus status, String ownerEmail) {
        TimeSlot slot = timeSlotRepository.findById(slotId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung giờ"));

        if (!slot.getField().getFacility().getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Không có quyền chỉnh sửa");
        }

        if (price != null) {
            slot.setPriceOverride(price);
        }


        if (status != null) {

            if (slot.getStatus() == SlotStatus.BOOKED && status != SlotStatus.BOOKED) {
                throw new IllegalStateException("Khung giờ đã có khách đặt, không thể đổi trạng thái trực tiếp");
            }
            slot.setStatus(status);
        }

        timeSlotRepository.save(slot);
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

    @Override
    public void updateStatus(Long slotId, SlotStatus status, String ownerEmail) {
        updateSlotDetail(slotId, null, status, ownerEmail);
    }

    @Override
    public void deleteSlot(Long slotId, String ownerEmail) {
        TimeSlot slot = timeSlotRepository.findById(slotId)
            .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));

        if (!slot.getField().getFacility().getOwner().getEmail().equals(ownerEmail)) {
            throw new SecurityException("Not authorized");
        }
        
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new IllegalStateException("Cannot delete a booked slot");
        }

        timeSlotRepository.delete(slot);
    }
}