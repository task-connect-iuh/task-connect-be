package vn.taskconnect.auth.api.dto;

import java.util.Set;
import java.util.UUID;
import vn.taskconnect.auth.api.AccountRole;
import vn.taskconnect.auth.api.AccountStatus;

/**
 * Thong tin toi thieu ve mot tai khoan, dung khi module khac can doc thong tin xac thuc
 * qua {@link vn.taskconnect.auth.api.AuthFacade}.
 */
public record AccountSummary(
        UUID accountId,
        String email,
        String phone,
        AccountStatus status,
        Set<AccountRole> roles
) {
}
