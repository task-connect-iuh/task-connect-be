package vn.taskconnect.user.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.taskconnect.auth.api.AuthFacade;
import vn.taskconnect.common.exception.BusinessException;
import vn.taskconnect.common.exception.ErrorCode;
import vn.taskconnect.user.api.LocationType;
import vn.taskconnect.user.api.SkillVerificationStatus;
import vn.taskconnect.user.dto.request.UpdateProfileRequest;
import vn.taskconnect.user.dto.response.AvailabilitySlotResponse;
import vn.taskconnect.user.dto.response.PublicProfileResponse;
import vn.taskconnect.user.dto.response.PublicVerifiedSkillResponse;
import vn.taskconnect.user.entity.PosterJobCategory;
import vn.taskconnect.user.entity.ServiceCategory;
import vn.taskconnect.user.entity.TaskerSkillProfile;
import vn.taskconnect.user.entity.UserProfile;
import vn.taskconnect.user.repository.PosterJobCategoryRepository;
import vn.taskconnect.user.repository.ServiceCategoryRepository;
import vn.taskconnect.user.repository.TaskerAvailabilityRepository;
import vn.taskconnect.user.repository.TaskerSkillProfileRepository;
import vn.taskconnect.user.repository.UserProfileRepository;

/**
 * Nghiep vu ho so ca nhan: xem, tao moi (lan dau) va cap nhat mot phan ho so cua chinh chu
 * tai khoan, va xem ho so toi thieu cong khai cua tai khoan khac (kem danh sach ky nang da
 * VERIFIED va lich ranh - xem getPublicProfile).
 */
@Service
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final TaskerSkillProfileRepository skillRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final TaskerAvailabilityRepository availabilityRepository;
    private final PosterJobCategoryRepository posterJobCategoryRepository;
    private final AuthFacade authFacade;
    private final Clock clock;

    public UserProfileService(UserProfileRepository profileRepository,
            TaskerSkillProfileRepository skillRepository, ServiceCategoryRepository categoryRepository,
            TaskerAvailabilityRepository availabilityRepository,
            PosterJobCategoryRepository posterJobCategoryRepository, AuthFacade authFacade, Clock clock) {
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
        this.categoryRepository = categoryRepository;
        this.availabilityRepository = availabilityRepository;
        this.posterJobCategoryRepository = posterJobCategoryRepository;
        this.authFacade = authFacade;
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
     * Danh sach id nhom dich vu Poster da khai la thuong thue, dung de ghep vao
     * ProfileResponse.jobCategoryIds - doc rong neu chua tung khai bao.
     */
    @Transactional(readOnly = true)
    public List<UUID> getMyJobCategoryIds(UUID accountId) {
        return posterJobCategoryRepository.findByAccountIdOrderByCreatedAtAsc(accountId).stream()
                .map(PosterJobCategory::getCategoryId)
                .toList();
    }

    /**
     * Doc ho so toi thieu cong khai cua mot tai khoan bat ky (dung khi xem trang ho so
     * nguoi khac), kem danh sach nhom dich vu da VERIFIED de FE hien badge "Da xac minh" va
     * lich ranh trong tuan de Poster biet Tasker ranh luc nao. Cung nem
     * USR-404-PROFILE_NOT_FOUND neu chua co ho so.
     */
    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(UUID accountId) {
        UserProfile profile = profileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
        List<AvailabilitySlotResponse> availability = availabilityRepository
                .findByAccountIdOrderByDayOfWeekAscStartTimeAsc(accountId).stream()
                .map(AvailabilitySlotResponse::from)
                .toList();
        return PublicProfileResponse.from(profile, verifiedSkillsOf(accountId), availability,
                authFacade.findAccount(accountId).orElse(null));
    }

    /**
     * Lap danh sach ky nang da VERIFIED cua mot tai khoan kem ten nhom dich vu - lay ten
     * theo lo (findAllById) thay vi truy van tung dong de tranh N+1 khi mot Tasker verified
     * nhieu nhom cung luc.
     */
    private List<PublicVerifiedSkillResponse> verifiedSkillsOf(UUID accountId) {
        List<TaskerSkillProfile> verified = skillRepository
                .findByAccountIdAndVerificationStatusOrderByVerifiedAtAsc(accountId, SkillVerificationStatus.VERIFIED);
        if (verified.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> categoryNameById = categoryRepository
                .findAllById(verified.stream().map(TaskerSkillProfile::getCategoryId).toList()).stream()
                .collect(Collectors.toMap(ServiceCategory::getId, ServiceCategory::getName));
        return verified.stream()
                .map(skill -> new PublicVerifiedSkillResponse(skill.getCategoryId(),
                        categoryNameById.get(skill.getCategoryId()), skill.getVerifiedAt()))
                .toList();
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
        replaceJobCategoriesIfPresent(accountId, request.jobCategoryIds(), now);
        UserProfile existing = profileRepository.findByAccountId(accountId).orElse(null);
        if (existing != null) {
            return applyPartialUpdate(existing, request, now);
        }

        String fullName = requireOnFirstCreate(request.fullName(), ErrorCode.MISSING_FULL_NAME);
        String operatingArea = requireOnFirstCreate(request.operatingArea(), ErrorCode.MISSING_OPERATING_AREA);
        UserProfile profile = new UserProfile(UUID.randomUUID(), accountId, fullName, operatingArea, now);
        profile.updateDetails(fullName, request.avatarUrl(), request.addressText(), request.bio(), operatingArea,
                request.locationLat(), request.locationLng(), request.preferredRadiusKm(), request.locationType(),
                request.arrivalNotes(), now);
        try {
            return profileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException ex) {
            UserProfile racedProfile = profileRepository.findByAccountId(accountId)
                    .orElseThrow(() -> ex);
            return applyPartialUpdate(racedProfile, request, now);
        }
    }

    /**
     * Neu jobCategoryIds co mat trong request (khac null), thay the toan bo danh sach nhom
     * dich vu Poster da khai bang danh sach moi - danh sach rong [] la cach xoa het lua
     * chon cu, khac voi null (khong doi). Validate moi id ton tai trong user_service_categories
     * truoc khi ghi, nem INVALID_JOB_CATEGORY neu co id la.
     *
     * <p>flush() ngay sau deleteByAccountId la bat buoc: deleteByAccountId chi queue lenh
     * DELETE trong persistence context (khong chay SQL ngay), va Hibernate mac dinh flush
     * INSERT truoc DELETE khi commit - neu khong flush som, insert lai mot category cu (vi
     * du giu nguyen 1 phan danh sach) se dung UNIQUE KEY
     * uq_user_poster_job_categories_account_category voi chinh dong sap bi xoa.
     */
    private void replaceJobCategoriesIfPresent(UUID accountId, List<UUID> jobCategoryIds, Instant now) {
        if (jobCategoryIds == null) {
            return;
        }
        List<UUID> distinctIds = jobCategoryIds.stream().distinct().toList();
        if (!distinctIds.isEmpty() && categoryRepository.findAllById(distinctIds).size() != distinctIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_JOB_CATEGORY);
        }
        posterJobCategoryRepository.deleteByAccountId(accountId);
        posterJobCategoryRepository.flush();
        List<PosterJobCategory> rows = distinctIds.stream()
                .map(categoryId -> new PosterJobCategory(UUID.randomUUID(), accountId, categoryId, now))
                .toList();
        posterJobCategoryRepository.saveAll(rows);
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
        String bio = request.bio() != null ? request.bio() : profile.getBio();
        String operatingArea = request.operatingArea() != null ? request.operatingArea() : profile.getOperatingArea();
        BigDecimal locationLat = request.locationLat() != null ? request.locationLat() : profile.getLocationLat();
        BigDecimal locationLng = request.locationLng() != null ? request.locationLng() : profile.getLocationLng();
        Integer preferredRadiusKm = request.preferredRadiusKm() != null ? request.preferredRadiusKm() : profile.getPreferredRadiusKm();
        LocationType locationType = request.locationType() != null ? request.locationType() : profile.getLocationType();
        String arrivalNotes = request.arrivalNotes() != null ? request.arrivalNotes() : profile.getArrivalNotes();

        if (Objects.equals(fullName, profile.getFullName()) && Objects.equals(avatarUrl, profile.getAvatarUrl())
                && Objects.equals(addressText, profile.getAddressText())
                && Objects.equals(bio, profile.getBio())
                && Objects.equals(operatingArea, profile.getOperatingArea())
                && isSameNumericValue(locationLat, profile.getLocationLat())
                && isSameNumericValue(locationLng, profile.getLocationLng())
                && Objects.equals(preferredRadiusKm, profile.getPreferredRadiusKm())
                && Objects.equals(locationType, profile.getLocationType())
                && Objects.equals(arrivalNotes, profile.getArrivalNotes())) {
            return profile;
        }

        profile.updateDetails(fullName, avatarUrl, addressText, bio, operatingArea, locationLat, locationLng,
                preferredRadiusKm, locationType, arrivalNotes, now);
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
