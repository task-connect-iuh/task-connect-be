package vn.taskconnect.auth.api.event;

import java.util.UUID;

/**
 * Doi email da thuc su hoan tat (auth_accounts.email da duoc cap nhat) - AuthService.
 * confirmEmailChange() phat su kien nay de Notification gui 2 thong bao: email CU nhan tin
 * bao da doi (kem email moi rut gon), email MOI nhan tin chuc mung. Khac 2 su kien OTP o
 * tren: khong con OTP nao trong payload, chi con dia chi email - van che trong toString()
 * theo cung nguyen tac voi cac su kien OTP.
 */
public record EmailChangedEvent(UUID accountId, String oldEmail, String newEmail) {

    @Override
    public String toString() {
        return "EmailChangedEvent[accountId=" + accountId + ", oldEmail=***, newEmail=***]";
    }
}
