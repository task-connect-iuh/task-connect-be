package vn.taskconnect.task.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.task.api.TaskApplicationStatus;

/**
 * Mot don ung tuyen nhin tu phia Poster (danh sach ung vien cua 1 cong viec), dung cho
 * GET /api/v1/tasks/{taskId}/applications.
 */
public record TaskApplicationResponse(
        UUID id,
        UUID taskerId,
        String taskerName,
        String taskerAvatarUrl,
        String proposedArrivalText,
        String message,
        TaskApplicationStatus status,
        Instant createdAt,
        Instant respondedAt
) {
}
