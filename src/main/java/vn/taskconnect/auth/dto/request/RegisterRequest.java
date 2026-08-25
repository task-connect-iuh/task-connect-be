package vn.taskconnect.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import vn.taskconnect.auth.api.AccountRole;

public record RegisterRequest(
        @NotBlank @Email String email,
        String phone,
        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường và 1 chữ số.")
        String password,
        @NotBlank String confirmPassword,
        @NotEmpty Set<AccountRole> roles
) {
}
