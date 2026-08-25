package vn.taskconnect.notification.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * The cho email o may dev khi chua co API key Brevo ({@code app.mail.enabled=false},
 * mac dinh). Khong gui gi ca, chi ghi log de xac nhan luong nghiep vu da chay toi day.
 *
 * <p>Khong log dia chi nguoi nhan day du (che con lai vai ky tu dau) va tuyet doi
 * khong log noi dung email/OTP - dung chinh cai vi pham rule 11 dang duoc go bo khoi
 * AuthService o thay doi nay.
 */
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String toAddress, String subject, String plainTextBody) {
        log.info("[DEV] Bo qua gui mail that (app.mail.enabled=false). Nguoi nhan: {}, tieu de: {}",
                mask(toAddress), subject);
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
