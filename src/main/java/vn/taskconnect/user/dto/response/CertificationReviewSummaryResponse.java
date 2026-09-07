package vn.taskconnect.user.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.user.api.CertificationStatus;
import vn.taskconnect.user.entity.ServiceCategory;
import vn.taskconnect.user.entity.TaskerCertification;
import vn.taskconnect.user.entity.UserProfile;

/**
 * Mot dong trong hang doi duyet chung chi (GET /users/tasker-certifications, chi
 * ROLE_ADMIN). Nhe hon CertificationReviewResponse - khong giai ma so hieu chung chi hay ky
 * presigned URL xem file, cung ly do voi KycReviewSummaryResponse. Mo chi tiet that su van
 * goi GET /users/{accountId}/tasker-skills/{categoryId}/certifications nhu cu.
 *
 * <p>{@code accountFullName}/{@code avatarUrl} lay tu user_profiles, {@code categoryName} lay
 * tu user_service_categories - ca hai chi de FE hien dung ten nguoi nop/ten nhom dich vu thay
 * vi accountId/categoryId dang UUID tho. Co the null neu tai khoan chua co ho so hoac category
 * (hiem, category bi xoa sau khi da nop) - FE tu fallback khi null.
 *
 * <p>{@code kycVerified} - trang thai KYC gan nhat cua tai khoan nop (VERIFIED hay khong),
 * de FE hien badge "Da xac thuc KYC" ngay canh ten thay vi Admin phai bam "Xem" tung dong moi
 * biet - Admin khong duyet duoc chung chi neu khong VERIFIED (xem
 * TaskerSkillService.requireKycVerified), nen bao truoc de tranh bam nham roi bi tu choi.
 */
public record CertificationReviewSummaryResponse(
        UUID id,
        UUID accountId,
        String accountFullName,
        String avatarUrl,
        UUID categoryId,
        String categoryName,
        UUID certificateTypeId,
        CertificationStatus status,
        Instant submittedAt,
        boolean kycVerified
) {

    /** Chuyen entity sang DTO tom tat, khong dung truong _enc nao. profile/category co the null. */
    public static CertificationReviewSummaryResponse from(TaskerCertification certification, UserProfile profile,
            ServiceCategory category, boolean kycVerified) {
        return new CertificationReviewSummaryResponse(
                certification.getId(),
                certification.getAccountId(),
                profile != null ? profile.getFullName() : null,
                profile != null ? profile.getAvatarUrl() : null,
                certification.getCategoryId(),
                category != null ? category.getName() : null,
                certification.getCertificateTypeId(),
                certification.getStatus(),
                certification.getSubmittedAt(),
                kycVerified);
    }
}
