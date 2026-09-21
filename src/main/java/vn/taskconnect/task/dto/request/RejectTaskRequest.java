package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ly do tu choi, dung cho PATCH /tasks/{taskId}/reject - bat buoc phai co, cung mau RejectKycRequest. */
public record RejectTaskRequest(@NotBlank @Size(max = 500) String rejectionReason) {
}
