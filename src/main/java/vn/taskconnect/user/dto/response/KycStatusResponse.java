package vn.taskconnect.user.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.user.api.KycStatus;
import vn.taskconnect.user.entity.KycVerification;

/**
 * Trang thai lan nop KYC gan nhat, tra ve cho chinh chu tai khoan (GET /users/me/kyc-verifications/latest).
 * Khong lo du lieu nhay cam (so CCCD, anh) - chinh chu tu nhap nen khong can hien lai.
 */
public record KycStatusResponse(
        UUID id,
        KycStatus status,
        Instant submittedAt,
        Instant reviewedAt,
        String rejectionReason
) {

    /** Chuyen entity sang DTO tra ve qua API, tranh entity JPA lo ra ngoai lop controller. */
    public static KycStatusResponse from(KycVerification verification) {
        return new KycStatusResponse(
                verification.getId(),
                verification.getStatus(),
                verification.getSubmittedAt(),
                verification.getReviewedAt(),
                verification.getRejectionReason());
    }
}
