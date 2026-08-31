package vn.taskconnect.auth.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.taskconnect.auth.entity.AuthPasswordResetToken;

public interface AuthPasswordResetTokenRepository extends JpaRepository<AuthPasswordResetToken, UUID> {

    /**
     * Ma con hieu luc moi nhat cua mot tai khoan. Khong co finder tra cuu rieng theo
     * token_hash: OTP 6 chu so tra cuu toan cuc theo hash se cho phep ke tan cong doan
     * dai mot ma bat ky roi khop trung ma cua bat ky tai khoan nao dang cho dat lai mat
     * khau. Tra cuu bat buoc dinh pham vi theo account_id.
     */
    Optional<AuthPasswordResetToken> findFirstByAccountIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID accountId);

    List<AuthPasswordResetToken> findByAccountIdAndUsedAtIsNull(UUID accountId);

    /**
     * Xoa hang loat ma da het han (bulk JPQL DELETE) - dung cho AuthTokenCleanupService
     * don dep dinh ky, xem AuthRefreshTokenRepository.deleteByExpiresAtBefore.
     *
     * @return so dong da xoa, dung de ghi log
     */
    @Modifying
    @Query("delete from AuthPasswordResetToken t where t.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);
}
