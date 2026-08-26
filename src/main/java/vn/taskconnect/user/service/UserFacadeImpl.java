package vn.taskconnect.user.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.taskconnect.user.api.UserFacade;
import vn.taskconnect.user.api.dto.UserProfileSummary;
import vn.taskconnect.user.repository.UserProfileRepository;

@Service
class UserFacadeImpl implements UserFacade {

    private final UserProfileRepository profileRepository;

    UserFacadeImpl(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
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
}
