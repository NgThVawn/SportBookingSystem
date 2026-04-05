package J2EE.SportBooingSystem.dto.response;

import J2EE.SportBooingSystem.entity.ExtraService;
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
public class ExtraServiceOptionResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private String unit;
    private String scope;
    private Integer stock;
    private String appliesToSportType;

    public static ExtraServiceOptionResponse from(ExtraService service) {
        boolean isCommon = service.getAppliesToSportType() == null;
        return ExtraServiceOptionResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .price(service.getPrice())
                .unit(service.getUnit())
                .stock(service.getStock())
                .scope(isCommon ? "COMMON" : "SPORT_SPECIFIC")
                .appliesToSportType(isCommon ? null : service.getAppliesToSportType().getDisplayName())
                .build();
    }
}
