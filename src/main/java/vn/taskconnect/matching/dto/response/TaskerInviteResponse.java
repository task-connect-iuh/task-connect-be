package vn.taskconnect.matching.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.matching.api.TaskerInviteStatus;

/**
 * Mot loi moi nhin tu ca hai phia (Poster vua tao, hoac Tasker xem/phan hoi) - mirror
 * TaskApplicationResponse.java cua module Task.
 */
public record TaskerInviteResponse(
        UUID id,
        UUID taskerId,
        String taskerName,
        String taskerAvatarUrl,
        TaskerInviteStatus status,
        Instant createdAt,
        Instant respondedAt
) {
}
