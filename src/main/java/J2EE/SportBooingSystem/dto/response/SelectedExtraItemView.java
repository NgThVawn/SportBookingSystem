package J2EE.SportBooingSystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectedExtraItemView {
    private Long serviceId;
    private String serviceName;
    private Integer quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}