package vn.taskconnect.user.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import vn.taskconnect.user.api.Gender;
import vn.taskconnect.user.api.KycStatus;

/**
 * Chi tiet day du mot lan nop KYC de Admin xet duyet (GET /users/{accountId}/kyc-verifications/latest,
 * chi ROLE_ADMIN). Khac KycStatusResponse - co so CCCD da giai ma va presigned GET URL ngan
 * han de xem anh mat truoc/sau, dung lam trong luc KycVerificationService xu ly giai ma va
 * ky URL, khong tu giai ma trong DTO. dateOfBirth/gender co the null cho cac ban ghi nop
 * truoc V16 (cot NULL-able, khong backfill duoc).
 */
public record KycReviewDetailResponse(
        UUID id,
        UUID accountId,
        String fullNameOnId,
        LocalDate dateOfBirth,
        Gender gender,
        String idNumber,
        String idCardFrontViewUrl,
        String idCardBackViewUrl,
        KycStatus status,
        Instant submittedAt,
        Instant reviewedAt,
        String rejectionReason
) {
}
