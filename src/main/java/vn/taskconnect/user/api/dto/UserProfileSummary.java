package vn.taskconnect.user.api.dto;

import java.util.UUID;
import vn.taskconnect.user.api.KycStatus;

/**
 * Thong tin toi thieu ve ho so ca nhan, dung khi module khac can doc ten/khu vuc hoat dong
 * qua {@link vn.taskconnect.user.api.UserFacade}.
 */
public record UserProfileSummary(
        UUID accountId,
        String fullName,
        String avatarUrl,
        String operatingArea,
        KycStatus kycStatus
) {
}
