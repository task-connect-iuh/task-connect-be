package vn.taskconnect.notification.infrastructure;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Gui email that qua SMTP relay (Brevo o production, xem .env.example). Chi duoc tao
 * khi {@code app.mail.enabled=true} - o may dev chua co API key Brevo,
 * {@link LoggingEmailSender} thay the, ung dung van khoi dong binh thuong.
 *
 * <p>{@code spring.mail.username}/{@code password} trong application.yml de default
 * rong (khac quy uoc fail-fast cua DB_PASSWORD/JWT_SECRET) vi Spring resolve placeholder
 * luc bind bat ke bean nay co duoc tao hay khong - dat khong default se sap context o
 * moi may dev chua co key. Fail-fast duoc chuyen xuong day, chi chay khi bean nay thuc
 * su duoc tao.
 */
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final NotificationMailProperties notificationMailProperties;

    SmtpEmailSender(JavaMailSender mailSender, MailProperties mailProperties,
            NotificationMailProperties notificationMailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.notificationMailProperties = notificationMailProperties;
    }

    @PostConstruct
    void requireCredentials() {
        String username = mailProperties.getUsername();
        String password = mailProperties.getPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "MAIL_USERNAME va MAIL_PASSWORD bat buoc khi app.mail.enabled=true (xem .env.example)");
        }
    }

    @Override
    public void send(String toAddress, String subject, String plainTextBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(notificationMailProperties.fromAddress(), notificationMailProperties.fromName());
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(plainTextBody, false);
            mailSender.send(message);
        } catch (MailException | UnsupportedEncodingException | MessagingException ex) {
            throw new EmailDeliveryException(ex);
        }
    }
}
