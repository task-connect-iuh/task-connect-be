package vn.taskconnect.user.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import vn.taskconnect.auth.api.dto.AccountSummary;
import vn.taskconnect.user.api.KycStatus;
import vn.taskconnect.user.entity.UserProfile;

/**
 * Ho so day du tra ve cho chinh chu tai khoan (GET/PATCH /users/me). email/phone doc tu
 * AuthAccount qua AuthFacade (module Auth) - khong luu trong user_profiles.
 */
public record ProfileResponse(
        UUID accountId,
        String fullName,
        String avatarUrl,
        String addressText,
        String bio,
        String operatingArea,
        BigDecimal locationLat,
        BigDecimal locationLng,
        KycStatus kycStatus,
        String email,
        String phone
) {

    /** Chuyen entity sang DTO tra ve qua API, tranh entity JPA lo ra ngoai lop controller. */
    public static ProfileResponse from(UserProfile profile, AccountSummary account) {
        return new ProfileResponse(
                profile.getAccountId(),
                profile.getFullName(),
                profile.getAvatarUrl(),
                profile.getAddressText(),
                profile.getBio(),
                profile.getOperatingArea(),
                profile.getLocationLat(),
                profile.getLocationLng(),
                profile.getKycStatus(),
                account != null ? account.email() : null,
                account != null ? account.phone() : null);
    }
}
