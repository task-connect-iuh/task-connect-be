package vn.taskconnect.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Thu hoi role ADMIN cua tai khoan co email nay - chi super-admin duoc goi. */
public record RevokeAdminRoleRequest(
        @NotBlank @Email String email
) {
}
