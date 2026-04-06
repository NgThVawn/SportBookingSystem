package J2EE.SportBooingSystem.dto.response;

import lombok.*;
import java.time.LocalTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AvailabilityResponse {
    private LocalTime openTime;
    private LocalTime closeTime;
    private List<OccupiedSlot> occupied;  // Các khung đã bị đặt hoặc chặn

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class OccupiedSlot {
        private LocalTime start;
        private LocalTime end;
        private String type; // "BOOKING" hoặc "BLOCKED"
    }
}