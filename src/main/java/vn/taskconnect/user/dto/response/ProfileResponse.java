package vn.taskconnect.user.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.auth.api.dto.AccountSummary;
import vn.taskconnect.user.api.KycStatus;
import vn.taskconnect.user.api.LocationType;
import vn.taskconnect.user.entity.UserProfile;

/**
 * Ho so day du tra ve cho chinh chu tai khoan (GET/PATCH /users/me). email/phone doc tu
 * AuthAccount qua AuthFacade (module Auth) - khong luu trong user_profiles. locationType/
 * arrivalNotes/jobCategoryIds chi Poster dung toi (phan "Gioi thieu ngan" tren
 * ProfilePage.tsx) - null/rong voi tai khoan chua khai bao, xem
 * V22__add_poster_fields_to_user_profiles.sql va V23__create_user_poster_job_categories.sql.
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
        Integer preferredRadiusKm,
        LocationType locationType,
        String arrivalNotes,
        List<UUID> jobCategoryIds,
        KycStatus kycStatus,
        String email,
        String phone
) {

    /** Chuyen entity sang DTO tra ve qua API, tranh entity JPA lo ra ngoai lop controller. */
    public static ProfileResponse from(UserProfile profile, AccountSummary account, List<UUID> jobCategoryIds) {
        return new ProfileResponse(
                profile.getAccountId(),
                profile.getFullName(),
                profile.getAvatarUrl(),
                profile.getAddressText(),
                profile.getBio(),
                profile.getOperatingArea(),
                profile.getLocationLat(),
                profile.getLocationLng(),
                profile.getPreferredRadiusKm(),
                profile.getLocationType(),
                profile.getArrivalNotes(),
                jobCategoryIds,
                profile.getKycStatus(),
                account != null ? account.email() : null,
                account != null ? account.phone() : null);
    }
}
