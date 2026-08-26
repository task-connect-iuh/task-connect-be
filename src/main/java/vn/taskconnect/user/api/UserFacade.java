package vn.taskconnect.user.api;

import java.util.Optional;
import java.util.UUID;
import vn.taskconnect.user.api.dto.UserProfileSummary;

/**
 * Be mat cong khai duy nhat cua module User. Module khac chi duoc goi qua day, cam import
 * entity trong {@code user.entity} hoac inject repository cua module User.
 */
public interface UserFacade {

    /**
     * Doc thong tin toi thieu ho so ca nhan theo accountId. Rong neu tai khoan chua tung
     * tao ho so (xem quyet dinh lazy-create trong docs/PROGRESS-USER-MODULE.md).
     */
    Optional<UserProfileSummary> findProfile(UUID accountId);
}
