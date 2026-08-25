package vn.taskconnect.notification.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh gui email, doc tu {@code app.mail.*} trong application.yml.
 *
 * <p>Dat ten {@code NotificationMailProperties}, KHONG dat ten {@code MailProperties}:
 * Spring Boot da co san lop {@code org.springframework.boot.autoconfigure.mail.MailProperties},
 * IDE auto-import nham lop do van bien dich duoc nhung chay sai hoan toan.
 *
 * <p>{@code spring.mail.username}/{@code password} (SMTP that) khac voi cap nay -
 * hai bien do dieu khien ket noi SMTP, con {@code enabled}/{@code fromAddress}/
 * {@code fromName} o day dieu khien nghiep vu gui hay khong gui va noi dung nguoi gui.
 */
@ConfigurationProperties(prefix = "app.mail")
public record NotificationMailProperties(boolean enabled, String fromAddress, String fromName) {
}
