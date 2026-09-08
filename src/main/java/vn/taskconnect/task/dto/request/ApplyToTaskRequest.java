package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Du lieu Tasker gui khi ung tuyen mot cong viec, dung cho POST /api/v1/tasks/{taskId}/applications.
 * Khong co truong gia de xuat (bo theo yeu cau nguoi dung, xem docs/PROGRESS-TASK-TASKER-MODULE.md).
 * proposedArrivalText cung da bo khoi form (yeu cau nguoi dung), gio la tuy chon - khong con
 * @NotBlank, FE hien tai khong con gui truong nay.
 */
public record ApplyToTaskRequest(
        @Size(max = 200) String proposedArrivalText,
        @Size(max = 2000) String message
) {
}
