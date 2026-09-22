package vn.taskconnect.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * @param phone So dien thoai dang dia phuong (0xxxxxxxxx) ung vien - kiem tra truoc khi gui
 *               OTP Firebase, xem AuthService.checkPhoneAvailable().
 */
public record CheckPhoneRequest(@NotBlank String phone) {
}
