package vn.taskconnect.auth.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.auth.api.AccountRole;
import vn.taskconnect.auth.entity.AuthAccountRole;

public interface AuthAccountRoleRepository extends JpaRepository<AuthAccountRole, UUID> {

    List<AuthAccountRole> findByAccountId(UUID accountId);

    /** Dung cho grantAdminRole - kiem tra tai khoan da co san role nay chua truoc khi insert. */
    boolean existsByAccountIdAndRole(UUID accountId, AccountRole role);

    /**
     * Dung cho revokeAdminRole - xoa dung 1 dong (account_id, role), khong load entity qua
     * vong doi Hibernate. Tra ve so dong bi xoa (0 hoac 1, nho UNIQUE KEY
     * uq_auth_account_roles_account_role) de service phan biet "khong co gi de xoa".
     */
    long deleteByAccountIdAndRole(UUID accountId, AccountRole role);
}
