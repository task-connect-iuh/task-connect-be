package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Du lieu Tasker gui khi bam "Nhan tin hoi them" (UC16 muc 2), dung cho
 * POST /api/v1/tasks/{taskId}/applications/inquire. message bat buoc (khac ApplyToTaskRequest)
 * vi day chinh la cau hoi Tasker muon gui - hanh dong bam nut nay da bao ham hanh dong "gui".
 */
public record InquireRequest(
        @NotBlank @Size(max = 2000) String message
) {
}
