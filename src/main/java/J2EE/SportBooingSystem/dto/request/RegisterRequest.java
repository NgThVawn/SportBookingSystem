package J2EE.SportBooingSystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Họ và tên là bắt buộc")
    @Size(min = 2, max = 100, message = "Tên phải từ 2–100 ký tự")
    private String fullName;

    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "Số điện thoại chỉ bao gồm số")
    private String phone;

    private boolean registerAsOwner = false;
}