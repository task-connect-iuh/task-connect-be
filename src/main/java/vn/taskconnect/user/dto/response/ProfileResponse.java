package vn.taskconnect.user.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import vn.taskconnect.user.api.KycStatus;
import vn.taskconnect.user.entity.UserProfile;

/**
 * Ho so day du tra ve cho chinh chu tai khoan (GET/PATCH /users/me).
 */
public record ProfileResponse(
        UUID accountId,
        String fullName,
        String avatarUrl,
        String addressText,
        String operatingArea,
        BigDecimal locationLat,
        BigDecimal locationLng,
        KycStatus kycStatus
) {

    /** Chuyen entity sang DTO tra ve qua API, tranh entity JPA lo ra ngoai lop controller. */
    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(
                profile.getAccountId(),
                profile.getFullName(),
                profile.getAvatarUrl(),
                profile.getAddressText(),
                profile.getOperatingArea(),
                profile.getLocationLat(),
                profile.getLocationLng(),
                profile.getKycStatus());
    }
}
