package vn.taskconnect.notification.infrastructure;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.taskconnect.auth.api.event.EmailChangeOldOtpRequestedEvent;
import vn.taskconnect.notification.api.NotificationFacade;
import vn.taskconnect.notification.api.dto.EmailOtpMessage;

/**
 * Cau noi giua su kien cua Auth va viec gui mail cua Notification, cho buoc 1 cua luong
 * doi email (xac minh email hien tai). Cung dieu kien AFTER_COMMIT + @Async nhu
 * EmailVerificationRequestedListener - xem Javadoc do de biet ly do.
 */
@Component
class EmailChangeOldOtpRequestedListener {

    private final NotificationFacade notificationFacade;

    EmailChangeOldOtpRequestedListener(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(EmailChangeOldOtpRequestedEvent event) {
        notificationFacade.sendEmailChangeOldOtp(new EmailOtpMessage(
                event.accountId(), event.email(), event.otp(), event.validFor()));
    }
}
