package vn.taskconnect.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Gan role ADMIN cho tai khoan co email nay - chi super-admin duoc goi. */
public record GrantAdminRoleRequest(
        @NotBlank @Email String email
) {
}
