package J2EE.SportBooingSystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BlockedTimeRequest {

    @NotNull private Long fieldId;

    @NotNull @FutureOrPresent
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Size(max = 255)
    private String reason;
}
