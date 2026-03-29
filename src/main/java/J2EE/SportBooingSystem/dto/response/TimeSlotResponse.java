package J2EE.SportBooingSystem.dto.response;

import J2EE.SportBooingSystem.entity.TimeSlot;
import J2EE.SportBooingSystem.enums.SlotStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TimeSlotResponse {
    private Long id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal price;
    private SlotStatus status;
    private boolean available;

    public static TimeSlotResponse from(TimeSlot ts) {
        TimeSlotResponse r = new TimeSlotResponse();
        r.setId(ts.getId());
        r.setDate(ts.getDate());
        r.setStartTime(ts.getStartTime());
        r.setEndTime(ts.getEndTime());
        r.setPrice(ts.getEffectivePrice());
        r.setStatus(ts.getStatus());
        r.setAvailable(ts.getStatus() == SlotStatus.AVAILABLE);
        return r;
    }
}
