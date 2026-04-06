package J2EE.SportBooingSystem.dto.request;


import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;


@Data
public class FacilityRequest {

    @NotBlank(message = "Facility name is required")
    @Size(max = 150)
    private String name;

    private String description;

    @NotBlank(message = "Address is required")
    @Size(max = 300)
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String district;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Invalid phone")
    private String phone;

    @Email(message = "Invalid email")
    private String email;

    @NotBlank(message = "Open time required")
    private String openTime;

    @NotBlank(message = "Close time required")
    private String closeTime;

    private List<MultipartFile> images;

    private List<Long> deletedImageIds;
}
