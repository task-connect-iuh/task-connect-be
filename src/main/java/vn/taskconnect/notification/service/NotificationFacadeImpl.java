package vn.taskconnect.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import vn.taskconnect.notification.api.NotificationFacade;
import vn.taskconnect.notification.api.dto.EmailChangedNotice;
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

    @Override
    public void sendPasswordResetOtp(EmailOtpMessage message) {
        try {
            emailSender.send(message.recipientEmail(),
                    template.passwordResetSubject(),
                    template.passwordResetBody(message.otp(), message.validFor()));
        } catch (EmailDeliveryException | MailException ex) {
            log.error("Gui ma dat lai mat khau that bai cho account {}: {}",
                    message.accountId(), ex.getMessage(), ex);
        }
    }

    @Override
    public void sendEmailChangeOldOtp(EmailOtpMessage message) {
        try {
            emailSender.send(message.recipientEmail(),
                    template.emailChangeOldOtpSubject(),
                    template.emailChangeOldOtpBody(message.otp(), message.validFor()));
        } catch (EmailDeliveryException | MailException ex) {
            log.error("Gui ma xac minh email hien tai (doi email) that bai cho account {}: {}",
                    message.accountId(), ex.getMessage(), ex);
        }
    }

    @Override
    public void sendEmailChangeNewOtp(EmailOtpMessage message) {
        try {
            emailSender.send(message.recipientEmail(),
                    template.emailChangeNewOtpSubject(),
                    template.emailChangeNewOtpBody(message.otp(), message.validFor()));
        } catch (EmailDeliveryException | MailException ex) {
            log.error("Gui ma xac minh email moi (doi email) that bai cho account {}: {}",
                    message.accountId(), ex.getMessage(), ex);
        }
    }

    @Override
    public void sendEmailChangedNotices(EmailChangedNotice notice) {
        try {
            emailSender.send(notice.oldEmail(), template.emailChangedOldNoticeSubject(),
                    template.emailChangedOldNoticeBody(maskEmail(notice.newEmail())));
        } catch (EmailDeliveryException | MailException ex) {
            log.error("Gui thong bao doi email (toi dia chi cu) that bai cho account {}: {}",
                    notice.accountId(), ex.getMessage(), ex);
        }
        try {
            emailSender.send(notice.newEmail(), template.emailChangedWelcomeSubject(),
                    template.emailChangedWelcomeBody());
        } catch (EmailDeliveryException | MailException ex) {
            log.error("Gui thong bao doi email (toi dia chi moi) that bai cho account {}: {}",
                    notice.accountId(), ex.getMessage(), ex);
        }
    }

    /**
     * Rut gon email de hien trong thong bao gui toi dia chi CU (vd "new***@gmail.com") -
     * khong lo toan bo email moi cho nguoi doc (co the la thiet bi/hop thu dung chung).
     * Giu 3 ky tu dau cua phan local-part (hoac ca phan do neu ngan hon 3 ky tu), thay phan
     * con lai bang "***", giu nguyen domain.
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        String visible = localPart.length() <= 3 ? localPart : localPart.substring(0, 3);
        return visible + "***" + domain;
    }
}
