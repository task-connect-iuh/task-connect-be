package vn.taskconnect.auth.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.taskconnect.auth.api.AccountStatus;
import vn.taskconnect.auth.entity.AuthAccount;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, UUID> {

    Optional<AuthAccount> findByEmail(String email);

    /** Tim tai khoan da tung dang nhap Google truoc do, dung de nhan biet lan dang nhap lai. */
    Optional<AuthAccount> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /** Kiem tra trung so dien thoai voi mot tai khoan KHAC accountId - dung khi doi phone (loai tru chinh minh). */
    boolean existsByPhoneAndIdNot(String phone, UUID id);

    /**
     * Lay danh sach id tai khoan qua han theo status va moc tao - dung cho
     * AuthAccountCleanupService de xoa truoc du lieu phu thuoc (vd user_profiles, qua
     * UserFacade) roi moi xoa chinh tai khoan bang deleteByIdIn(), tranh loi FK RESTRICT.
     */
    @Query("select a.id from AuthAccount a where a.status = :status and a.createdAt < :cutoff")
    List<UUID> findIdsByStatusAndCreatedAtBefore(@Param("status") AccountStatus status, @Param("cutoff") Instant cutoff);

    /**
     * Xoa hang loat tai khoan theo danh sach id cu the (bulk JPQL DELETE, khong load tung
     * entity qua vong doi Hibernate) - dung sau khi da xoa xong du lieu phu thuoc gan voi
     * cung danh sach id nay (xem findIdsByStatusAndCreatedAtBefore). ON DELETE CASCADE tren
     * auth_account_roles/auth_refresh_tokens/auth_email_verification_tokens (xem
     * V1__create_auth_tables.sql) tu don theo, khong de lai dong mo coi.
     *
     * @return so tai khoan da xoa, dung de ghi log
     */
    @Modifying
    @Query("delete from AuthAccount a where a.id in :ids")
    int deleteByIdIn(@Param("ids") Collection<UUID> ids);
}
