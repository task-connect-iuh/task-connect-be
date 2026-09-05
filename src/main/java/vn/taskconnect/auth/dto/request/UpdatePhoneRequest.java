package vn.taskconnect.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdatePhoneRequest(@NotBlank String phone) {
}
