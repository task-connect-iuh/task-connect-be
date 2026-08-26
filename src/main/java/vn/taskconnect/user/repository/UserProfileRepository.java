package vn.taskconnect.user.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.user.entity.UserProfile;

/**
 * Truy xuat du lieu bang user_profiles. Chi module User duoc inject truc tiep repository
 * nay - module khac phai goi qua UserFacade.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /** Tim ho so theo id tai khoan - accountId la unique key, toi da mot ho so moi tai khoan. */
    Optional<UserProfile> findByAccountId(UUID accountId);
}
