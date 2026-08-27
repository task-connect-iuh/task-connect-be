package vn.taskconnect.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.user.dto.request.UpdateProfileRequest;
import vn.taskconnect.user.entity.UserProfile;
import vn.taskconnect.user.repository.ServiceCategoryRepository;
import vn.taskconnect.user.repository.UserProfileRepository;

/**
 * Unit test thuan tuy (khong DB, khong Spring context) cho UserProfileService, dung
 * Mockito mock UserProfileRepository. Bao phu cac test case UC03-01, 03, 05, 06, 07, 08,
 * 09, 16, 17, 18 trong docs/QA-REPORT-USER-STEP1-PROFILE.md. Cac case con lai (UC03-02, 04,
 * 10, 11, 19 doan DB that, 20-23) can integration/system test that Testcontainers, xem
 * ghi chu trong file bao cao QA ve tinh trang Docker/Testcontainers tren may hien tai.
 */
class UserProfileServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final Instant FIXED_NOW = Instant.parse("2026-08-26T10:00:00Z");

    private final UserProfileRepository repository = mock(UserProfileRepository.class);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private final UserProfileService service = new UserProfileService(repository, clock);

    private static UpdateProfileRequest requestOf(String fullName, String avatarUrl, String addressText,
            String operatingArea, BigDecimal lat, BigDecimal lng) {
        return new UpdateProfileRequest(fullName, avatarUrl, addressText, operatingArea, lat, lng);
    }

    // UC03-01: PATCH lan dau, du truong bat buoc -> tao moi ho so.
    @Test
    void should_createProfile_when_firstPatchHasFullNameAndOperatingArea() {
        when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = requestOf("Nguyen Van A", null, null, "Quan 7, TP.HCM", null, null);
        UserProfile result = service.upsertProfile(ACCOUNT_ID, request);

        assertThat(result.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(result.getOperatingArea()).isEqualTo("Quan 7, TP.HCM");
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        verify(repository, times(1)).saveAndFlush(any(UserProfile.class));
    }

    // UC03-05: thieu fullName o lan tao dau tien.
    @Test
    void should_throwMissingFullName_when_firstPatchHasNoFullName() {
        when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        UpdateProfileRequest request = requestOf(null, null, null, "Quan 7, TP.HCM", null, null);

        assertThatThrownBy(() -> service.upsertProfile(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.MISSING_FULL_NAME);
        verify(repository, never()).saveAndFlush(any());
    }

    // UC03-06: thieu operatingArea (chuoi trang) o lan tao dau tien.
    @Test
    void should_throwMissingOperatingArea_when_firstPatchHasBlankOperatingArea() {
        when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        UpdateProfileRequest request = requestOf("Nguyen Van A", null, null, "   ", null, null);

        assertThatThrownBy(() -> service.upsertProfile(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.MISSING_OPERATING_AREA);
        verify(repository, never()).saveAndFlush(any());
    }

    // UC03-03: PATCH mot phan, chi gui avatarUrl - cac truong khac giu nguyen.
    @Test
    void should_keepOtherFields_when_patchOnlySendsAvatarUrl() {
        UserProfile existing = new UserProfile(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A", "Quan 7",
                FIXED_NOW.minusSeconds(3600));
        existing.updateDetails("Nguyen Van A", "old-avatar.png", "123 Le Loi", "Quan 7",
                BigDecimal.valueOf(10.75), BigDecimal.valueOf(106.66), FIXED_NOW.minusSeconds(3600));
        when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = requestOf(null, "new-avatar.png", null, null, null, null);
        UserProfile result = service.upsertProfile(ACCOUNT_ID, request);

        assertThat(result.getAvatarUrl()).isEqualTo("new-avatar.png");
        assertThat(result.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(result.getAddressText()).isEqualTo("123 Le Loi");
        assertThat(result.getOperatingArea()).isEqualTo("Quan 7");
        assertThat(result.getLocationLat()).isEqualByComparingTo("10.75");
        assertThat(result.getUpdatedAt()).isEqualTo(FIXED_NOW);
    }

    // UC03-18: PATCH voi toan bo field null tren ho so da co - khong duoc dung save()/updatedAt.
    @Test
    void should_notTouchUpdatedAt_when_patchBodyChangesNothing() {
        Instant originalUpdatedAt = FIXED_NOW.minusSeconds(3600);
        UserProfile existing = new UserProfile(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A", "Quan 7",
                originalUpdatedAt);
        existing.updateDetails("Nguyen Van A", "avatar.png", "123 Le Loi", "Quan 7",
                BigDecimal.valueOf(10.75), BigDecimal.valueOf(106.66), originalUpdatedAt);
        when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(existing));

        UpdateProfileRequest emptyRequest = requestOf(null, null, null, null, null, null);
        UserProfile result = service.upsertProfile(ACCOUNT_ID, emptyRequest);

        assertThat(result.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        verify(repository, never()).save(any());
    }

    // UC03-17: goi PATCH giong het request hai lan lien tiep - idempotent, khong doi them lan hai.
    @Test
    void should_returnSameData_when_patchCalledTwiceWithSamePayload() {
        UserProfile existing = new UserProfile(UUID.randomUUID(), ACCOUNT_ID, "Nguyen Van A", "Quan 7", FIXED_NOW);
        when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = requestOf("Nguyen Van A", "avatar.png", null, "Quan 7", null, null);
        UserProfile first = service.upsertProfile(ACCOUNT_ID, request);
        UserProfile second = service.upsertProfile(ACCOUNT_ID, request);

        assertThat(first.getAvatarUrl()).isEqualTo(second.getAvatarUrl());
        assertThat(first.getFullName()).isEqualTo(second.getFullName());
    }

    // UC03-07: GET /me khi chua tung PATCH -> USR-404-PROFILE_NOT_FOUND.
    @Test
    void should_throwProfileNotFound_when_gettingOwnProfileBeforeFirstPatch() {
        when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProfile(ACCOUNT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.PROFILE_NOT_FOUND);
    }

    // UC03-08: GET /{accountId} cong khai khi tai khoan chua co ho so -> cung ma loi.
    @Test
    void should_throwProfileNotFound_when_gettingPublicProfileWithoutAnyRow() {
        UUID otherAccountId = UUID.randomUUID();
        when(repository.findByAccountId(otherAccountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicProfile(otherAccountId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.PROFILE_NOT_FOUND);
    }

    // UC03-16: hai PATCH lan dau gan nhu dong thoi cho cung accountId - request thua race
    // gap DataIntegrityViolationException khi insert, phai tu chuyen sang cap nhat ban ghi
    // vua duoc request kia tao ra, khong duoc de loi vo tung nguoi dung.
    @Test
    void should_fallBackToUpdate_when_concurrentFirstPatchRaceHitsUniqueConstraint() {
        UserProfile racedProfile = new UserProfile(UUID.randomUUID(), ACCOUNT_ID, "Nguoi Thang Race", "Quan 1",
                FIXED_NOW);
        when(repository.findByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(racedProfile));
        when(repository.saveAndFlush(any(UserProfile.class)))
                .thenThrow(new DataIntegrityViolationException("uq_user_profiles_account"));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = requestOf("Nguoi Thua Race", null, null, "Quan 3", null, null);
        UserProfile result = service.upsertProfile(ACCOUNT_ID, request);

        assertThat(result.getFullName()).isEqualTo("Nguoi Thua Race");
        assertThat(result.getOperatingArea()).isEqualTo("Quan 3");
        verify(repository, times(1)).save(any(UserProfile.class));
    }

    // UC03-09: UserFacade.findProfile khi chua co ho so -> Optional rong, khong throw.
    @Test
    void should_returnEmptyOptional_when_facadeFindsNoProfile() {
        UserFacadeImpl facade = new UserFacadeImpl(repository, mock(ServiceCategoryRepository.class));
        when(repository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThat(facade.findProfile(ACCOUNT_ID)).isEmpty();
    }
}
