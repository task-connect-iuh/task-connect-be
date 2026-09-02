package vn.taskconnect.user.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.user.api.KycStatus;
import vn.taskconnect.user.entity.KycVerification;
import vn.taskconnect.user.entity.UserProfile;

/**
 * Mot dong trong hang doi duyet KYC (GET /users/kyc-verifications, chi ROLE_ADMIN). Nhe hon
 * KycReviewDetailResponse - khong giai ma so CCCD hay ky presigned URL xem anh, vi ca trang
 * co the co hang chuc dong ma phan lon admin chi can xem qua roi duyet, khong mo chi tiet
 * tung dong. Mo chi tiet that su van goi GET /users/{accountId}/kyc-verifications/latest
 * (dung accountId cua dong nay) nhu cu.
 *
 * <p>{@code accountFullName}/{@code avatarUrl} lay tu user_profiles cua chinh tai khoan
 * nop ho so - khac voi {@code fullNameOnId} (ten tu go tay tren CCCD, co the khac chinh ta
 * hoac chua khop voi ten dang ky). FE dung 2 truong nay de hien dung nguoi + avatar that o
 * cot "Nguoi gui", tranh nham voi ten CCCD (xem lai truoc do FE dang hien nham fullNameOnId
 * lam ten hien thi chinh). Co the null neu tai khoan chua tung tao ho so (hiem, vi ho so
 * gio duoc tao ngay luc dang ky - xem AuthService.register()).
 */
public record KycReviewSummaryResponse(
        UUID id,
        UUID accountId,
        String fullNameOnId,
        String accountFullName,
        String avatarUrl,
        KycStatus status,
        Instant submittedAt
) {

    /** Chuyen entity sang DTO tom tat, khong dung truong _enc nao. profile co the null. */
    public static KycReviewSummaryResponse from(KycVerification verification, UserProfile profile) {
        return new KycReviewSummaryResponse(
                verification.getId(),
                verification.getAccountId(),
                verification.getFullNameOnId(),
                profile != null ? profile.getFullName() : null,
                profile != null ? profile.getAvatarUrl() : null,
                verification.getStatus(),
                verification.getSubmittedAt());
    }
}
