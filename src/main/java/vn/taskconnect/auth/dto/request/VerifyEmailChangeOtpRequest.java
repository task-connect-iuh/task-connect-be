package vn.taskconnect.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Dung chung cho ca 2 buoc xac minh OTP cua luong doi email (email cu va email moi). */
public record VerifyEmailChangeOtpRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Mã xác minh gồm 6 chữ số.") String otp
) {
}
