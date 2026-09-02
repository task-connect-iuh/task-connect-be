package vn.taskconnect.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Doi mat khau khi da dang nhap - can mat khau hien tai, khac ResetPasswordRequest (dung OTP email, khong dang nhap). */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường và 1 chữ số.")
        String newPassword,
        @NotBlank String confirmNewPassword
) {
}
