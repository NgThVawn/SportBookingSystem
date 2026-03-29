package J2EE.SportBooingSystem.dto.request;

import J2EE.SportBooingSystem.enums.SportType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FieldRequest {

    @NotBlank(message = "Field name is required")
    @Size(max = 100)
    private String name;

    @NotNull(message = "Sport type is required")
    private SportType sportType;

    private String description;

    @Size(max = 50)
    private String surfaceType;

    private Integer capacity;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal pricePerHour;

    private BigDecimal pricePerSlot;

    @NotNull(message = "Slot duration is required")
    @Min(value = 30, message = "Minimum slot duration is 30 minutes")
    private Integer slotDuration;
}
