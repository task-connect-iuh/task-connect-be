package vn.taskconnect.auth.api.event;

import java.time.Duration;
import java.util.UUID;

/**
 * Yeu cau gui ma xac minh email. Su kien Spring noi bo trong cung tien trinh, khong
 * qua RabbitMQ, khong ghi outbox, khong roi JVM. Auth publish, module Notification lang
 * nghe (xem EmailVerificationRequestedListener).
 *
 * <p>DEVIATION co chu y so voi .claude/rules/13-async-messaging.md ("Cam dua du lieu
 * dinh danh ca nhan hoac token vao payload"): payload o day bat buoc mang email nguoi
 * nhan va ma OTP vi day chinh la noi dung can gui. Rui ro ma rule do chan (payload nam
 * tren duong truyen mang, trong bang outbox, trong log cua broker) khong ton tai voi
 * mot Spring ApplicationEvent noi bo. Bu lai, toString() che ca hai truong nhay cam
 * de tranh log vo tinh o cap DEBUG. Da xac nhan voi nguoi dung.
 */
public record EmailVerificationRequestedEvent(UUID accountId, String email, String otp, Duration validFor) {

    @Override
    public String toString() {
        return "EmailVerificationRequestedEvent[accountId=" + accountId + ", email=***, otp=***]";
    }
}
