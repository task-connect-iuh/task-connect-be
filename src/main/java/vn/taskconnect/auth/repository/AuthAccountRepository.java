package vn.taskconnect.auth.repository;

import java.time.Instant;
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

    /**
     * Xoa hang loat tai khoan qua han theo status va moc tao (bulk JPQL DELETE, khong
     * load tung entity qua vong doi Hibernate) - dung cho AuthAccountCleanupService don
     * dep dinh ky tai khoan UNVERIFIED bo do khong bao gio xac minh email. ON DELETE
     * CASCADE tren auth_account_roles/auth_refresh_tokens/auth_email_verification_tokens
     * (xem V1__create_auth_tables.sql) tu don theo, khong de lai dong mo coi.
     *
     * @return so tai khoan da xoa, dung de ghi log
     */
    @Modifying
    @Query("delete from AuthAccount a where a.status = :status and a.createdAt < :cutoff")
    int deleteByStatusAndCreatedAtBefore(@Param("status") AccountStatus status, @Param("cutoff") Instant cutoff);
}
