package vn.taskconnect.user.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import vn.taskconnect.user.api.CertificationStatus;

/**
 * Chi tiet mot lan nop chung chi. Dung chung cho hai endpoint: Admin xet duyet (GET
 * /users/{accountId}/tasker-skills/{categoryId}/certifications, ROLE_ADMIN) va chinh chu
 * Tasker tu xem lai qua nut "Xem chi tiet" (GET /users/me/tasker-skills/{categoryId}/
 * certifications, ROLE_TASKER). Co so hieu chung chi da giai ma va presigned GET URL ngan
 * han de xem file - dung trong luc TaskerSkillService xu ly giai ma va ky URL, khong tu
 * giai ma trong DTO.
 */
public record CertificationReviewResponse(
        UUID id,
        UUID certificateTypeId,
        String certificateNumber,
        String issuingAuthority,
        LocalDate issuedDate,
        LocalDate expiryDate,
        String fileViewUrl,
        String experienceProofUrl,
        Integer claimedExperienceYears,
        CertificationStatus status,
        String rejectionReason,
        Instant submittedAt,
        Instant reviewedAt
) {
}
