package vn.taskconnect.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;
import java.util.UUID;
import vn.taskconnect.auth.api.AccountRole;
import vn.taskconnect.auth.api.AccountStatus;

/**
 * @param status       trang thai tai khoan hien tai. UI dung truong nay de hien nhac nho
 *                     "chua xac minh email" khi status la UNVERIFIED - dang nhap van thanh
 *                     cong binh thuong voi status nay, chi bi chan khi LOCKED hoac SUSPENDED.
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
