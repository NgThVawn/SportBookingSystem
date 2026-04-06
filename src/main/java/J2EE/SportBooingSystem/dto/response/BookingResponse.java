package J2EE.SportBooingSystem.dto.response;

import J2EE.SportBooingSystem.entity.Booking;
import J2EE.SportBooingSystem.enums.BookingStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingResponse {
    private Long id;
    private String bookingCode;
    private String customerName;
    private String fieldName;
    private String facilityName;
    private String facilityAddress;
    private String sportType;
    private String facilityImageUrl;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal fieldPrice;
    private BigDecimal extraTotal;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private String note;
    private Boolean isReviewed;
    private Long facilityId;
    private List<ExtraItem> extraItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExtraItem {
        private String serviceName;
        private Integer quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    public static BookingResponse from(Booking b) {
        List<ExtraItem> extraItems = b.getExtraServices() == null ? Collections.emptyList() :
            b.getExtraServices().stream()
                .map(item -> ExtraItem.builder()
                            .serviceName(item.getServiceName() != null ? item.getServiceName() : "Dịch vụ")
                            .quantity(item.getQuantity() != null ? item.getQuantity() : 0)
                            .unit(item.getUnit() != null ? item.getUnit() : "đơn vị")
                            .unitPrice(item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO)
                            .subtotal(item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                    .build())
                .toList();

        BigDecimal extraTotal = extraItems.stream()
                .map(ExtraItem::getSubtotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrice = b.getTotalPrice() == null ? BigDecimal.ZERO : b.getTotalPrice();
        BigDecimal fieldPrice = totalPrice.subtract(extraTotal);
        if (fieldPrice.compareTo(BigDecimal.ZERO) < 0) {
            fieldPrice = BigDecimal.ZERO;
        }

        return BookingResponse.builder()
                .id(b.getId())
                .bookingCode(b.getBookingCode())
                .customerName(b.getUser().getFullName())
                .fieldName(b.getField().getName())
                .facilityName(b.getField().getFacility().getName())
                .facilityAddress(b.getField().getFacility().getAddress())
                .sportType(b.getField().getSportType().getDisplayName())
                .facilityImageUrl(b.getField().getFacility().getPrimaryImageUrl())
                .bookingDate(b.getBookingDate())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .fieldPrice(fieldPrice)
                .extraTotal(extraTotal)
                .totalPrice(totalPrice)
                .status(b.getStatus())
                .note(b.getNote())
                .isReviewed(b.getIsReviewed())
                .facilityId(b.getField().getFacility().getId())
                .extraItems(extraItems)
                .build();
    }
}
