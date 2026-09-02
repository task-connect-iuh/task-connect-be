package vn.taskconnect.user.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.dto.ServiceCategorySummary;
import vn.taskconnect.user.api.dto.UserProfileSummary;
import vn.taskconnect.user.repository.ServiceCategoryRepository;
import vn.taskconnect.user.repository.UserProfileRepository;

@Service
class UserFacadeImpl implements UserFacade {

    private final UserProfileRepository profileRepository;
    private final ServiceCategoryRepository categoryRepository;

    UserFacadeImpl(UserProfileRepository profileRepository, ServiceCategoryRepository categoryRepository) {
        this.profileRepository = profileRepository;
        this.categoryRepository = categoryRepository;
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
}
