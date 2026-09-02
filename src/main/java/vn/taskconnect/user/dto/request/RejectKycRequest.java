package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ly do tu choi, dung cho PATCH /users/kyc-verifications/{id}/reject - bat buoc phai co. */
public record RejectKycRequest(@NotBlank @Size(max = 500) String rejectionReason) {
}
