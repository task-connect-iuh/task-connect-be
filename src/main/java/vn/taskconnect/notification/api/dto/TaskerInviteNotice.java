package vn.taskconnect.notification.api.dto;

import java.util.UUID;

/**
 * Thong tin can de bao Tasker biet ho vua duoc Poster moi lam mot cong viec (module Matching,
 * xem TaskerInviteCreatedEvent). DTO lien module cua Notification (xem
 * .claude/rules/10-module-boundary.md) - Matching khong tu goi EmailSender, chi publish su
 * kien roi listener cua Notification chuyen thanh message nay.
 */
public record TaskerInviteNotice(UUID accountId, String recipientEmail, String taskTitle) {

    /** Che email nguoi nhan, cung ly do voi EmailOtpMessage - tranh log lo du lieu ca nhan. */
    @Override
    public String toString() {
        return "TaskerInviteNotice[accountId=" + accountId + ", recipientEmail=***, taskTitle=" + taskTitle + "]";
    }
}
