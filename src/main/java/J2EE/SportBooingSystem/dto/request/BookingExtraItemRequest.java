package J2EE.SportBooingSystem.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingExtraItemRequest {

    @NotNull(message = "Dịch vụ đi kèm không hợp lệ")
    private Long serviceId;

    @NotNull(message = "Số lượng dịch vụ không hợp lệ")
    @Min(value = 1, message = "Số lượng dịch vụ phải từ 1")
    private Integer quantity;
}
