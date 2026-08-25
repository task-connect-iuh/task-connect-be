package vn.taskconnect.auth.api;

import java.util.Optional;
import java.util.UUID;
import vn.taskconnect.auth.api.dto.AccountSummary;

/**
 * Be mat cong khai duy nhat cua module Auth. Module khac chi duoc goi qua day,
 * cam import entity trong {@code auth.entity} hoac inject repository cua module Auth.
 */
public interface AuthFacade {

    /**
     * Doc thong tin toi thieu cua mot tai khoan theo id. Rong neu khong ton tai.
     */
    Optional<AccountSummary> findAccount(UUID accountId);
}
