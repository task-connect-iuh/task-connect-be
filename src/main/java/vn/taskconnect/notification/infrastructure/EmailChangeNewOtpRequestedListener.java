package vn.taskconnect.notification.infrastructure;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.taskconnect.auth.api.event.EmailChangeNewOtpRequestedEvent;
import vn.taskconnect.notification.api.NotificationFacade;
import vn.taskconnect.notification.api.dto.EmailOtpMessage;

/**
 * Cau noi giua su kien cua Auth va viec gui mail cua Notification, cho buoc 2 cua luong
 * doi email (xac minh email moi). Cung dieu kien AFTER_COMMIT + @Async nhu
 * EmailVerificationRequestedListener - xem Javadoc do de biet ly do.
 */
@Component
class EmailChangeNewOtpRequestedListener {

    private final NotificationFacade notificationFacade;

    EmailChangeNewOtpRequestedListener(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(EmailChangeNewOtpRequestedEvent event) {
        notificationFacade.sendEmailChangeNewOtp(new EmailOtpMessage(
                event.accountId(), event.newEmail(), event.otp(), event.validFor()));
    }
}
