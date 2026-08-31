package vn.taskconnect.auth.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Kiem tra hop dong toString() cua event: email va OTP khong duoc xuat hien trong chuoi,
 * cung ly do voi EmailVerificationRequestedEventTest.
 */
class PasswordResetRequestedEventTest {

    @Test
    void should_maskEmailAndOtp_when_convertingToString() {
        String email = "nguoi-dung-that@example.com";
        String otp = "123456";
        UUID accountId = UUID.randomUUID();

        String text = new PasswordResetRequestedEvent(accountId, email, otp, Duration.ofMinutes(5)).toString();

        assertThat(text)
                .contains(accountId.toString())
                .doesNotContain(email)
                .doesNotContain(otp);
    }
}
