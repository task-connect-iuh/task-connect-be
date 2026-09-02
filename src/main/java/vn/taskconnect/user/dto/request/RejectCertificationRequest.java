package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ly do tu choi, dung cho PATCH /users/tasker-certifications/{id}/reject - bat buoc phai co. */
public record RejectCertificationRequest(@NotBlank @Size(max = 500) String rejectionReason) {
}
