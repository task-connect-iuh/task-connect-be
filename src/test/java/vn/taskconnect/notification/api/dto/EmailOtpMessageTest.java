package vn.taskconnect.notification.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmailOtpMessageTest {

    @Test
    void should_maskRecipientAndOtp_when_convertingToString() {
        String email = "nguoi-dung-that@example.com";
        String otp = "123456";
        UUID accountId = UUID.randomUUID();

        String text = new EmailOtpMessage(accountId, email, otp, Duration.ofMinutes(5)).toString();

        assertThat(text)
                .contains(accountId.toString())
                .doesNotContain(email)
                .doesNotContain(otp);
    }
}
