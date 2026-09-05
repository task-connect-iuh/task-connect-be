package vn.taskconnect.auth.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.auth.entity.AuthEmailChangeToken;

public interface AuthEmailChangeTokenRepository extends JpaRepository<AuthEmailChangeToken, UUID> {

    /** Doc dong "dang hieu luc" duy nhat cua mot tai khoan - xem AuthEmailChangeToken javadoc. */
    Optional<AuthEmailChangeToken> findByAccountId(UUID accountId);

    /** Xoa dong cu (neu co) truoc khi bat dau mot lan doi email moi tu dau. */
    void deleteByAccountId(UUID accountId);
}
