package J2EE.SportBooingSystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank
    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
    private String password;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;
}
