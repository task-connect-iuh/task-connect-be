package vn.taskconnect.user.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.user.dto.request.UpdateProfileRequest;
import vn.taskconnect.user.entity.UserProfile;
import vn.taskconnect.user.repository.UserProfileRepository;

/**
 * Nghiep vu ho so ca nhan: xem, tao moi (lan dau) va cap nhat mot phan ho so cua chinh chu
 * tai khoan, va xem ho so toi thieu cong khai cua tai khoan khac.
 */
@Service
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final Clock clock;

    public UserProfileService(UserProfileRepository profileRepository, Clock clock) {
        this.profileRepository = profileRepository;
        this.clock = clock;
    }

    /**
     * Doc ho so cua chinh chu tai khoan. Nem USR-404-PROFILE_NOT_FOUND neu tai khoan chua
     * tung goi PATCH /users/me lan nao (xem quyet dinh lazy-create trong
     * docs/PROGRESS-USER-MODULE.md).
     */
    @Transactional(readOnly = true)
    public UserProfile getMyProfile(UUID accountId) {
        return profileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
    }

    /**
     * Doc ho so toi thieu cong khai cua mot tai khoan bat ky (dung khi xem trang ho so
     * nguoi khac). Cung nem USR-404-PROFILE_NOT_FOUND neu chua co ho so.
     */
    @Transactional(readOnly = true)
    public UserProfile getPublicProfile(UUID accountId) {
        return profileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
    }

    /**
     * Ap dung PATCH mot phan dung nghia 16-api-contract.md: field nao trong request la
     * null thi giu nguyen gia tri cu, khong field nao bi bat buoc phai gui lai moi lan.
     * Neu day la lan tao ho so dau tien (chua co ban ghi nao), fullName va operatingArea
     * bat buoc phai co gia tri vi la NOT NULL trong schema - kiem tra thu cong o day thay
     * vi Bean Validation, vi rang buoc nay chi ap dung khi chua co ho so.
     *
     * <p>Hai request PATCH dau tien gan nhu dong thoi cho cung mot tai khoan co the cung
     * thay chua co ho so va cung insert - UNIQUE KEY uq_user_profiles_account chan lai o
     * DB, saveAndFlush() bat DataIntegrityViolationException ngay tai day va tu chuyen
     * sang cap nhat ban ghi vua duoc request kia tao ra, thay vi tra loi xung dot cho chinh
     * chu tai khoan cua ho so do.
     */
    @Transactional
    public UserProfile upsertProfile(UUID accountId, UpdateProfileRequest request) {
        Instant now = clock.instant();
        UserProfile existing = profileRepository.findByAccountId(accountId).orElse(null);
        if (existing != null) {
            return applyPartialUpdate(existing, request, now);
        }

        String fullName = requireOnFirstCreate(request.fullName(), ErrorCode.MISSING_FULL_NAME);
        String operatingArea = requireOnFirstCreate(request.operatingArea(), ErrorCode.MISSING_OPERATING_AREA);
        UserProfile profile = new UserProfile(UUID.randomUUID(), accountId, fullName, operatingArea, now);
        profile.updateDetails(fullName, request.avatarUrl(), request.addressText(), operatingArea,
                request.locationLat(), request.locationLng(), now);
        try {
            return profileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException ex) {
            UserProfile racedProfile = profileRepository.findByAccountId(accountId)
                    .orElseThrow(() -> ex);
            return applyPartialUpdate(racedProfile, request, now);
        }
    }

    /**
     * Ghi de len profile hien co chi voi field nao co mat trong request (khac null); field
     * null nghia la "khong doi", giu nguyen gia tri dang luu. Neu sau khi ap dung khong co
     * field nao thuc su thay doi gia tri (vi du PATCH voi body rong), bo qua save() va
     * khong dung updatedAt - tranh "cham" ban ghi ma khong co thay doi du lieu thuc su.
     */
    private UserProfile applyPartialUpdate(UserProfile profile, UpdateProfileRequest request, Instant now) {
        String fullName = request.fullName() != null ? request.fullName() : profile.getFullName();
        String avatarUrl = request.avatarUrl() != null ? request.avatarUrl() : profile.getAvatarUrl();
        String addressText = request.addressText() != null ? request.addressText() : profile.getAddressText();
        String operatingArea = request.operatingArea() != null ? request.operatingArea() : profile.getOperatingArea();
        BigDecimal locationLat = request.locationLat() != null ? request.locationLat() : profile.getLocationLat();
        BigDecimal locationLng = request.locationLng() != null ? request.locationLng() : profile.getLocationLng();

        if (Objects.equals(fullName, profile.getFullName()) && Objects.equals(avatarUrl, profile.getAvatarUrl())
                && Objects.equals(addressText, profile.getAddressText())
                && Objects.equals(operatingArea, profile.getOperatingArea())
                && isSameNumericValue(locationLat, profile.getLocationLat())
                && isSameNumericValue(locationLng, profile.getLocationLng())) {
            return profile;
        }

        profile.updateDetails(fullName, avatarUrl, addressText, operatingArea, locationLat, locationLng, now);
        return profileRepository.save(profile);
    }

    /**
     * So sanh hai BigDecimal theo gia tri so hoc (compareTo), khong theo scale, vi
     * "90.0" va "90.0000000" phai duoc coi la khong doi du khac nhau ve scale luu tru.
     */
    private boolean isSameNumericValue(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    /**
     * Bat buoc field phai co gia tri khac rong khi day la lan tao ho so dau tien.
     */
    private String requireOnFirstCreate(String value, ErrorCode errorCode) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(errorCode);
        }
        return value;
    }
}
