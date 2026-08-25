package vn.taskconnect.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import vn.taskconnect.notification.api.NotificationFacade;
import vn.taskconnect.notification.api.dto.EmailOtpMessage;
import vn.taskconnect.notification.infrastructure.EmailDeliveryException;
import vn.taskconnect.notification.infrastructure.EmailOtpTemplate;
import vn.taskconnect.notification.infrastructure.EmailSender;

@Service
class NotificationFacadeImpl implements NotificationFacade {

    private static final Logger log = LoggerFactory.getLogger(NotificationFacadeImpl.class);

    private final EmailSender emailSender;
    private final EmailOtpTemplate template;

    NotificationFacadeImpl(EmailSender emailSender, EmailOtpTemplate template) {
        this.emailSender = emailSender;
        this.template = template;
    }

    @Override
    public void sendEmailVerificationOtp(EmailOtpMessage message) {
        try {
            emailSender.send(message.recipientEmail(),
                    template.verificationSubject(),
                    template.verificationBody(message.otp(), message.validFor()));
        } catch (EmailDeliveryException | MailException ex) {
            // accountId la UUID, khong phai du lieu dinh danh ca nhan - duoc phep log.
            // Khong log dia chi email, khong log OTP (rule 11). Khong nem lai: nguoi dung
            // da nhan phan hoi thanh cong tu request HTTP truoc do, khac phuc bang chuc
            // nang gui lai ma.
            log.error("Gui ma xac minh email that bai cho account {}: {}",
                    message.accountId(), ex.getMessage(), ex);
        }
    }
}
