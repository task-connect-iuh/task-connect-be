package vn.taskconnect.user.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.user.api.CertificationStatus;
import vn.taskconnect.user.entity.TaskerCertification;

/**
 * Mot dong trong hang doi duyet chung chi (GET /users/tasker-certifications, chi
 * ROLE_ADMIN). Nhe hon CertificationReviewResponse - khong giai ma so hieu chung chi hay ky
 * presigned URL xem file, cung ly do voi KycReviewSummaryResponse. Mo chi tiet that su van
 * goi GET /users/{accountId}/tasker-skills/{categoryId}/certifications nhu cu.
 */
public record CertificationReviewSummaryResponse(
        UUID id,
        UUID accountId,
        UUID categoryId,
        UUID certificateTypeId,
        CertificationStatus status,
        Instant submittedAt
) {

    /** Chuyen entity sang DTO tom tat, khong dung truong _enc nao. */
    public static CertificationReviewSummaryResponse from(TaskerCertification certification) {
        return new CertificationReviewSummaryResponse(
                certification.getId(),
                certification.getAccountId(),
                certification.getCategoryId(),
                certification.getCertificateTypeId(),
                certification.getStatus(),
                certification.getSubmittedAt());
    }
}
