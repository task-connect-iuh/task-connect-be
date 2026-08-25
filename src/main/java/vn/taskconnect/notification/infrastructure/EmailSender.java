package vn.taskconnect.notification.infrastructure;

/**
 * Diem noi giua module Notification va SMTP that. Tach rieng interface nay de test co
 * the mock ma khong can khoi dong SMTP server that (GreenMail, MailHog...).
 */
public interface EmailSender {

    void send(String toAddress, String subject, String plainTextBody);
}
