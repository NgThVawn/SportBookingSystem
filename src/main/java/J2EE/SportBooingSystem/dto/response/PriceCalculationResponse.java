package J2EE.SportBooingSystem.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceCalculationResponse {
    private BigDecimal totalPrice;
    private List<PriceSegment> segments;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PriceSegment {
        private String timeRange;       // VD: "17:00 – 19:00"
        private String ruleName;        // VD: "Giờ cao điểm tối"
        private BigDecimal pricePerHour;
        private double hours;
        private BigDecimal subtotal;
    }
}
