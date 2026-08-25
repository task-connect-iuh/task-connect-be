package vn.taskconnect.auth.dto.response;

/**
 * Luon tra ve cung mot gia tri {@code retryAfterSeconds} co dinh bat ke email co ton
 * tai, da xac minh, hay dang trong cooldown hay khong - tranh lo thong tin email nao
 * da dang ky (xem Javadoc AuthService.resendVerification).
 */
public record ResendVerificationResponse(int retryAfterSeconds) {
}
