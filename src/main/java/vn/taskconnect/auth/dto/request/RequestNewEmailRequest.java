package vn.taskconnect.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestNewEmailRequest(@NotBlank @Email String newEmail) {
}
