package vn.taskconnect.auth.api.event;

import java.time.Duration;
import java.util.UUID;

/**
 * Yeu cau gui ma OTP xac minh quyen so huu email HIEN TAI - buoc 1 cua luong doi email
 * (AuthService.requestEmailChange()). Cung deviation ve payload nhu
 * EmailVerificationRequestedEvent - da xac nhan voi nguoi dung.
 */
public record EmailChangeOldOtpRequestedEvent(UUID accountId, String email, String otp, Duration validFor) {

    @Override
    public String toString() {
        return "EmailChangeOldOtpRequestedEvent[accountId=" + accountId + ", email=***, otp=***]";
    }
}
