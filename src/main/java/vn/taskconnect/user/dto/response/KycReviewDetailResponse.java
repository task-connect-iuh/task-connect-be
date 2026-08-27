package vn.taskconnect.user.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.user.api.KycStatus;

/**
 * Chi tiet day du mot lan nop KYC de Admin xet duyet (GET /users/{accountId}/kyc-verifications/latest,
 * chi ROLE_ADMIN). Khac KycStatusResponse - co so CCCD da giai ma va presigned GET URL ngan
 * han de xem anh mat truoc/sau, dung lam trong luc KycVerificationService xu ly giai ma va
 * ky URL, khong tu giai ma trong DTO.
 */
public record KycReviewDetailResponse(
        UUID id,
        UUID accountId,
        String fullNameOnId,
        String idNumber,
        String idCardFrontViewUrl,
        String idCardBackViewUrl,
        KycStatus status,
        Instant submittedAt,
        Instant reviewedAt,
        String rejectionReason
) {
}
