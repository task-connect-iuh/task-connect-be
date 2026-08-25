package vn.taskconnect.notification.api.dto;

import java.time.Duration;
import java.util.UUID;

/**
 * Yeu cau gui mot ma OTP qua email. DTO lien module cua Notification (xem
 * .claude/rules/10-module-boundary.md).
 */
public record EmailOtpMessage(UUID accountId, String recipientEmail, String otp, Duration validFor) {

    /**
     * Che nguoi nhan va noi dung ma: rule 11 cam log du lieu dinh danh ca nhan va noi
     * dung token/OTP, ma logging.level.vn.taskconnect dang la DEBUG nen record nay co
     * the bi log nguyen doi tuong o dau do.
     */
    @Override
    public String toString() {
        return "EmailOtpMessage[accountId=" + accountId + ", recipientEmail=***, otp=***]";
    }
}
