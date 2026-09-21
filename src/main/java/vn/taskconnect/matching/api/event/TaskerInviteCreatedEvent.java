package vn.taskconnect.matching.api.event;

import java.util.UUID;

/**
 * Loi moi moi duoc Poster tao ra cho mot Tasker. Su kien Spring noi bo trong cung tien
 * trinh, khong qua RabbitMQ - mirror EmailVerificationRequestedEvent (module Auth). Matching
 * publish, module Notification lang nghe (xem TaskerInviteCreatedListener) de bao Tasker
 * biet ho vua duoc moi.
 */
public record TaskerInviteCreatedEvent(UUID inviteId, UUID taskId, UUID taskerId, UUID posterId) {
}
