package vn.taskconnect.auth.api.event;

import java.time.Duration;
import java.util.UUID;

/**
 * Yeu cau gui ma OTP xac minh quyen so huu email MOI - buoc 2 cua luong doi email, chi phat
 * sau khi email hien tai da duoc xac minh xong (AuthService.requestNewEmailForChange()).
 */
public record EmailChangeNewOtpRequestedEvent(UUID accountId, String newEmail, String otp, Duration validFor) {

    @Override
    public String toString() {
        return "EmailChangeNewOtpRequestedEvent[accountId=" + accountId + ", newEmail=***, otp=***]";
    }
}
