package vn.taskconnect.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;
import java.util.UUID;
import vn.taskconnect.auth.api.AccountRole;
import vn.taskconnect.auth.api.AccountStatus;

/**
 * @param status       trang thai tai khoan hien tai. Voi login(), gia tri thuc te luon la
 *                     ACTIVE (UNVERIFIED/LOCKED/SUSPENDED da bi chan truoc khi cap token).
 *                     Voi refresh(), truong nay chi phan anh lai status hien co trong DB,
 *                     khong tu chan token moi neu tai khoan bi SUSPENDED sau khi phien da
 *                     mo - refresh() chua kiem tra lai status (ngoai pham vi thay doi nay).
 * @param refreshToken khong bao gio serialize ra client ({@code @JsonIgnore}) - theo
 *                     16-api-contract.md, refresh token chi duoc truyen qua cookie
 *                     {@code httpOnly}. AuthController van doc duoc gia tri nay tu object
 *                     Java de dung Set-Cookie, chi khong lo ra JSON body.
 */
public record TokenResponse(
        String accessToken,
        @JsonIgnore String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UUID accountId,
        AccountStatus status,
        Set<AccountRole> roles
) {
}
