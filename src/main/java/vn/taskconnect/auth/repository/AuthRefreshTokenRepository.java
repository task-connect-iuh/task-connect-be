package vn.taskconnect.auth.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.taskconnect.auth.entity.AuthRefreshToken;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, UUID> {

    Optional<AuthRefreshToken> findByTokenHash(String tokenHash);

    /**
     * Dung rieng cho AuthService.refresh(): khoa dong ngay luc doc (SELECT ... FOR UPDATE)
     * de chan race condition khi 2 request refresh cung mot token toi gan nhu dong thoi
     * (StrictMode goi effect 2 lan, nhieu tab, network retry...). Khong co khoa nay, ca hai
     * request deu doc duoc revoked_at = NULL truoc khi request kia kip commit, dan den ca
     * hai cung rotate thanh cong tu 1 token goc - sinh ra 2 token moi con hieu luc song song
     * thay vi 1. Request thu hai se bi block toi khi request dau tien commit, sau do doc lai
     * thay revoked_at da duoc set va bi tu choi dung nhu mong doi.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AuthRefreshToken t where t.tokenHash = :tokenHash")
    Optional<AuthRefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /**
     * Toan bo phien dang hieu luc cua mot tai khoan - dung de thu hoi hang loat khi dat
     * lai mat khau (xem AuthService.resetPassword).
     */
    List<AuthRefreshToken> findByAccountIdAndRevokedAtIsNull(UUID accountId);

    /**
     * Xoa hang loat token da het han (bulk JPQL DELETE, khong load tung entity qua
     * vong doi Hibernate) - dung cho AuthTokenCleanupService don dep dinh ky, tranh
     * bang phinh vo han vi day la bang append-only, khong co dong nao tu xoa.
     *
     * @return so dong da xoa, dung de ghi log
     */
    @Modifying
    @Query("delete from AuthRefreshToken t where t.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);
}
