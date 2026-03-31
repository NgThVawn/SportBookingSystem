package J2EE.SportBooingSystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TimeSlotGenerateRequest {

    @NotNull(message = "Field is required")
    private Long fieldId;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotNull(message = "Day start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime dayStartTime;

    @NotNull(message = "Day end time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime dayEndTime;

    private BigDecimal priceOverride;
}
