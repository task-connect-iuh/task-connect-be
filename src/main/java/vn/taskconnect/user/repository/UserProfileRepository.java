package vn.taskconnect.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Xoa hang loat ho so theo danh sach accountId (bulk JPQL DELETE, khong load tung
     * entity) - dung boi UserFacadeImpl.deleteProfilesByAccountIds() de Auth don dep du
     * lieu phu thuoc truoc khi xoa tai khoan UNVERIFIED qua han, tranh loi FK RESTRICT tu
     * fk_user_profiles_account (xem AuthAccountCleanupService).
     *
     * @return so ho so da xoa, dung de ghi log
     */
    @Modifying
    @Query("delete from UserProfile p where p.accountId in :accountIds")
    int deleteByAccountIdIn(@Param("accountIds") Collection<UUID> accountIds);
}
