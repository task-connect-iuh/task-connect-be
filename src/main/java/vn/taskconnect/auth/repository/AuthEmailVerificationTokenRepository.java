package vn.taskconnect.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.auth.entity.AuthEmailVerificationToken;

public interface AuthEmailVerificationTokenRepository extends JpaRepository<AuthEmailVerificationToken, UUID> {

    /**
     * Ma con hieu luc moi nhat cua mot tai khoan. Khong co finder tra cuu rieng theo
     * token_hash: OTP 6 chu so tra cuu toan cuc theo hash se cho phep ke tan cong doan
     * dai mot ma bat ky roi khop trung ma cua bat ky tai khoan nao dang cho xac minh.
     * Tra cuu bat buoc dinh pham vi theo account_id.
     */
    Optional<AuthEmailVerificationToken> findFirstByAccountIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID accountId);

    List<AuthEmailVerificationToken> findByAccountIdAndUsedAtIsNull(UUID accountId);
}
