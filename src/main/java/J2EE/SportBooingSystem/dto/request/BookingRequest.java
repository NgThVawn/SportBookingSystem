package J2EE.SportBooingSystem.dto.request;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BookingRequest {

    @NotNull(message = "Vui lòng chọn sân")
    private Long fieldId;

    @NotNull(message = "Vui lòng chọn ngày")
    @FutureOrPresent(message = "Ngày đặt không được trong quá khứ")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    @NotNull(message = "Vui lòng chọn giờ bắt đầu")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Vui lòng chọn giờ kết thúc")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Size(max = 500)
    private String note;

    private List<@Valid BookingExtraItemRequest> extraItems = new ArrayList<>();
}
