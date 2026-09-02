package vn.taskconnect.user.dto.response;

import java.util.UUID;

/**
 * Mot chung chi duoc chap nhan cho mot category cu the, tra ve cho client (GET
 * /users/service-categories/{categoryId}/certificate-requirements) - dung de FE hien danh
 * sach chung chi Tasker duoc chon upload khi khai bao ky nang cho category do (Buoc 6).
 * QUAN HE OR giua cac dong mandatory=true cua cung mot category - xem
 * V5__seed_user_certificate_types.sql va docs/PROGRESS-USER-MODULE.md Buoc 6.
 */
public record CategoryCertificateRequirementResponse(
        UUID certificateTypeId,
        String certificateTypeCode,
        String certificateTypeName,
        boolean mandatory,
        int minExperienceYears
) {
}
