package vn.taskconnect.user.dto.response;

import java.util.UUID;
import vn.taskconnect.user.entity.UserProfile;

/**
 * Ho so toi thieu, khong nhay cam, dung khi mot tai khoan xem ho so cua nguoi khac (vi du
 * Task/Review sau nay hien thi ten va khu vuc hoat dong cua Tasker). Khong lo addressText
 * chi tiet hay toa do chinh xac.
 */
public record PublicProfileResponse(
        UUID accountId,
        String fullName,
        String avatarUrl,
        String operatingArea
) {

    /** Chuyen entity sang DTO cong khai, chi lay cac truong khong nhay cam. */
    public static PublicProfileResponse from(UserProfile profile) {
        return new PublicProfileResponse(
                profile.getAccountId(),
                profile.getFullName(),
                profile.getAvatarUrl(),
                profile.getOperatingArea());
    }
}
