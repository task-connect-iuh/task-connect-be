package vn.taskconnect.notification.infrastructure;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.taskconnect.auth.api.event.EmailChangedEvent;
import vn.taskconnect.notification.api.NotificationFacade;
import vn.taskconnect.notification.api.dto.EmailChangedNotice;

/**
 * Cau noi giua su kien cua Auth va viec gui mail cua Notification, khi doi email da hoan
 * tat. Cung dieu kien AFTER_COMMIT + @Async nhu EmailVerificationRequestedListener - xem
 * Javadoc do de biet ly do.
 */
@Component
class EmailChangedListener {

    private final NotificationFacade notificationFacade;

    EmailChangedListener(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(EmailChangedEvent event) {
        notificationFacade.sendEmailChangedNotices(
                new EmailChangedNotice(event.accountId(), event.oldEmail(), event.newEmail()));
    }
}
