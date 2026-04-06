package J2EE.SportBooingSystem.dto.request;

import J2EE.SportBooingSystem.enums.DayType;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PriceRuleRequest {

    @NotBlank(message = "Tên quy tắc không được để trống")
    @Size(max = 100)
    private String name;

    @NotNull(message = "Vui lòng chọn loại ngày")
    private DayType dayType;

    @NotNull(message = "Vui lòng nhập giờ bắt đầu")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Vui lòng nhập giờ kết thúc")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @NotNull(message = "Vui lòng nhập giá")
    @DecimalMin(value = "0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal pricePerHour;

    @Min(0)
    private Integer priority = 0;
}
