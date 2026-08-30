package vn.taskconnect.notification.infrastructure;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.taskconnect.auth.api.event.PasswordResetRequestedEvent;
import vn.taskconnect.notification.api.NotificationFacade;
import vn.taskconnect.notification.api.dto.EmailOtpMessage;

/**
 * Cau noi giua su kien cua Auth va viec gui mail cua Notification, cho luong dat lai mat
 * khau. Cung dieu kien AFTER_COMMIT + @Async nhu EmailVerificationRequestedListener - xem
 * Javadoc do de biet ly do.
 */
@Component
class PasswordResetRequestedListener {

    private final NotificationFacade notificationFacade;

    PasswordResetRequestedListener(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(PasswordResetRequestedEvent event) {
        notificationFacade.sendPasswordResetOtp(new EmailOtpMessage(
                event.accountId(), event.email(), event.otp(), event.validFor()));
    }
}
