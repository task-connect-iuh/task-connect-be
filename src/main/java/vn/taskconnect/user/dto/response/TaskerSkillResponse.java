package vn.taskconnect.user.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.user.api.CertificationStatus;
import vn.taskconnect.user.api.SkillVerificationStatus;

/**
 * Mot ho so ky nang (theo category) cua chinh chu tai khoan, tra ve cho
 * GET /users/me/tasker-skills. Kem trang thai cua lan nop chung chi gan nhat de Tasker biet
 * ly do neu bi tu choi, khong can goi them API rieng. latestCertificationId de FE goi duoc
 * PATCH /users/me/tasker-certifications/{id}/cancel khi dang PENDING_REVIEW, khong can goi
 * rieng getCertificationsForReview chi de lay id.
 */
public record TaskerSkillResponse(
        UUID categoryId,
        int yearsExperience,
        Long priceMin,
        Long priceMax,
        SkillVerificationStatus verificationStatus,
        Instant verifiedAt,
        UUID latestCertificationId,
        CertificationStatus latestCertificationStatus,
        String latestCertificationRejectionReason
) {
}
