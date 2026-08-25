package vn.taskconnect.auth.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.auth.entity.AuthAccountRole;

public interface AuthAccountRoleRepository extends JpaRepository<AuthAccountRole, UUID> {

    List<AuthAccountRole> findByAccountId(UUID accountId);
}
