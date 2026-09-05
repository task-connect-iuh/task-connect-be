package vn.taskconnect.notification.api.dto;

import java.util.UUID;

/** Thong bao doi email da hoan tat, gui toi ca dia chi cu va moi. DTO lien module cua Notification. */
public record EmailChangedNotice(UUID accountId, String oldEmail, String newEmail) {

    @Override
    public String toString() {
        return "EmailChangedNotice[accountId=" + accountId + ", oldEmail=***, newEmail=***]";
    }
}
