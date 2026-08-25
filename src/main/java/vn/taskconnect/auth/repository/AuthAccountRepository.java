package vn.taskconnect.auth.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.auth.entity.AuthAccount;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, UUID> {

    Optional<AuthAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
