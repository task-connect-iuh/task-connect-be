package vn.taskconnect.user.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.user.api.KycStatus;
import vn.taskconnect.user.entity.KycVerification;

/**
 * Mot dong trong hang doi duyet KYC (GET /users/kyc-verifications, chi ROLE_ADMIN). Nhe hon
 * KycReviewDetailResponse - khong giai ma so CCCD hay ky presigned URL xem anh, vi ca trang
 * co the co hang chuc dong ma phan lon admin chi can xem qua roi duyet, khong mo chi tiet
 * tung dong. Mo chi tiet that su van goi GET /users/{accountId}/kyc-verifications/latest
 * (dung accountId cua dong nay) nhu cu.
 */
public record KycReviewSummaryResponse(
        UUID id,
        UUID accountId,
        String fullNameOnId,
        KycStatus status,
        Instant submittedAt
) {

    /** Chuyen entity sang DTO tom tat, khong dung truong _enc nao. */
    public static KycReviewSummaryResponse from(KycVerification verification) {
        return new KycReviewSummaryResponse(
                verification.getId(),
                verification.getAccountId(),
                verification.getFullNameOnId(),
                verification.getStatus(),
                verification.getSubmittedAt());
    }
}
