package J2EE.SportBooingSystem.dto.response;

import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.enums.BookingStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingResponse {
    private Long id;
    private String bookingCode;
    private String fieldName;
    private String facilityName;
    private String facilityAddress;
    private String sportType;
    private String facilityImageUrl;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private String note;

    public static BookingResponse from(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .bookingCode(b.getBookingCode())
                .fieldName(b.getField().getName())
                .facilityName(b.getField().getFacility().getName())
                .facilityAddress(b.getField().getFacility().getAddress())
                .sportType(b.getField().getSportType().getDisplayName())
                .facilityImageUrl(b.getField().getFacility().getPrimaryImageUrl())
                .bookingDate(b.getBookingDate())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .totalPrice(b.getTotalPrice())
                .status(b.getStatus())
                .note(b.getNote())
                .build();
    }
}
