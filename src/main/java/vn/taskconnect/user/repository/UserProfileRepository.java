package vn.taskconnect.user.repository;

import java.util.Collection;
import java.util.List;
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

    /** Tim theo lo nhieu accountId cung luc - tranh N+1 khi enrich mot trang danh sach (vd hang doi KYC). */
    List<UserProfile> findByAccountIdIn(Collection<UUID> accountIds);
}
