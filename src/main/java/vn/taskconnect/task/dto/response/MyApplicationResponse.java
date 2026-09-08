package vn.taskconnect.task.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.api.TaskStatus;

/**
 * Mot don ung tuyen nhin tu phia Tasker (man "Viec da nhan"), dung cho
 * GET /api/v1/tasks/applications/mine. Gom ca thong tin toi thieu cua cong viec de FE khong
 * phai goi them request rieng cho tung dong.
 */
public record MyApplicationResponse(
        UUID applicationId,
        TaskApplicationStatus status,
        String proposedArrivalText,
        String message,
        Instant createdAt,
        Instant respondedAt,
        UUID taskId,
        String taskTitle,
        String taskAddressText,
        Long taskBudgetAmount,
        Instant taskScheduledAt,
        TaskStatus taskStatus,
        UUID categoryId,
        String categoryName,
        String posterName,
        List<String> taskImageUrls
) {
}
