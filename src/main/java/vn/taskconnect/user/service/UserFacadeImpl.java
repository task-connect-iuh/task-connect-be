package vn.taskconnect.user.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.dto.ServiceCategorySummary;
import vn.taskconnect.user.api.dto.UserProfileSummary;
import vn.taskconnect.user.entity.UserProfile;
import vn.taskconnect.user.repository.ServiceCategoryRepository;
import vn.taskconnect.user.repository.UserProfileRepository;

@Service
class UserFacadeImpl implements UserFacade {

    private final UserProfileRepository profileRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final Clock clock;

    UserFacadeImpl(UserProfileRepository profileRepository, ServiceCategoryRepository categoryRepository,
            Clock clock) {
        this.profileRepository = profileRepository;
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    /** Doc ho so va anh xa sang UserProfileSummary de tra cho module khac qua UserFacade. */
    @Override
    public Optional<UserProfileSummary> findProfile(UUID accountId) {
        return profileRepository.findByAccountId(accountId)
                .map(profile -> new UserProfileSummary(
                        profile.getAccountId(),
                        profile.getFullName(),
                        profile.getAvatarUrl(),
                        profile.getOperatingArea(),
                        profile.getKycStatus()));
    }

    /** Doc danh muc con hien hanh va anh xa sang ServiceCategorySummary cho module khac. */
    @Override
    public List<ServiceCategorySummary> listActiveServiceCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(category -> new ServiceCategorySummary(
                        category.getId(), category.getCode(), category.getName(), category.getMinExperienceYears()))
                .toList();
    }

    /**
     * Goi tu Auth ngay sau khi tao tai khoan (xem AuthService.register()) de ho ten nhap o
     * form dang ky hien dung ngay tu lan dang nhap dau, thay vi cho toi khi nguoi dung tu
     * vao trang Ho so bam luu lan dau (lazy-create cu). operatingArea de rong ("") - van
     * thoa NOT NULL cua schema, nguoi dung dien sau. Bo qua neu da co ho so: idempotent,
     * phong truong hop su kien duoc goi lai (vd retry).
     */
    @Override
    @Transactional
    public void createInitialProfile(UUID accountId, String fullName) {
        if (profileRepository.findByAccountId(accountId).isPresent()) {
            return;
        }
        Instant now = clock.instant();
        profileRepository.save(new UserProfile(UUID.randomUUID(), accountId, fullName, "", now));
    }
}
