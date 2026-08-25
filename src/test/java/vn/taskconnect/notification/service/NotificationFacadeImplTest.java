package vn.taskconnect.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.taskconnect.notification.api.dto.EmailOtpMessage;
import vn.taskconnect.notification.infrastructure.EmailDeliveryException;
import vn.taskconnect.notification.infrastructure.EmailOtpTemplate;
import vn.taskconnect.notification.infrastructure.EmailSender;

/**
 * NotificationFacadeImpl khong bao gio duoc nem loi ra ngoai: nguoi goi la mot listener
 * bat dong bo (EmailVerificationRequestedListener), khong con request HTTP nao de bao
 * loi ve - nguoi dung da nhan phan hoi thanh cong tu truoc do.
 */
class NotificationFacadeImplTest {

    @Test
    void should_swallowAndLog_when_emailSenderThrowsDeliveryException() {
        EmailSender emailSender = mock(EmailSender.class);
        doThrow(new EmailDeliveryException(new RuntimeException("SMTP timeout")))
                .when(emailSender).send(any(), any(), any());
        NotificationFacadeImpl facade = new NotificationFacadeImpl(emailSender, new EmailOtpTemplate());

        EmailOtpMessage message = new EmailOtpMessage(
                UUID.randomUUID(), "user@example.com", "123456", Duration.ofMinutes(5));

        assertThatCode(() -> facade.sendEmailVerificationOtp(message)).doesNotThrowAnyException();
        verify(emailSender).send(any(), any(), any());
    }
}
