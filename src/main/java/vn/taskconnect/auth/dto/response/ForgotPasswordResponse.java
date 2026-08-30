package vn.taskconnect.auth.dto.response;

/**
 * Luon tra ve cung mot gia tri {@code retryAfterSeconds} co dinh bat ke email co ton tai,
 * tai khoan co bi SUSPENDED, hay dang trong cooldown hay khong - tranh lo thong tin email
 * nao da dang ky (xem Javadoc AuthService.forgotPassword).
 */
public record ForgotPasswordResponse(int retryAfterSeconds) {
}
