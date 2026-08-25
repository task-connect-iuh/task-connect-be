package vn.taskconnect.notification.infrastructure;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.taskconnect.auth.api.event.EmailVerificationRequestedEvent;
import vn.taskconnect.notification.api.NotificationFacade;
import vn.taskconnect.notification.api.dto.EmailOtpMessage;

/**
 * Cau noi giua su kien cua Auth va viec gui mail cua Notification.
 *
 * <p>AFTER_COMMIT: chi gui mail sau khi ma OTP da chac chan nam trong DB, tranh gui mail
 * cho mot giao dich sau do bi rollback. {@code @Async}: SMTP chay tren luong rieng
 * (notificationExecutor), khong nam trong transaction nghiep vu (rule "Cam goi API ben
 * ngoai ben trong @Transactional") va khong cong them vao thoi gian phan hoi API.
 *
 * <p>Luu y: {@code fallbackExecution} giu mac dinh false, nghia la neu su kien nay duoc
 * publish TU MOT METHOD KHONG CO @Transactional dang chay, listener se KHONG chay -
 * im lang, khong loi. Moi noi publish EmailVerificationRequestedEvent bat buoc phai
 * nam trong mot method @Transactional dang mo.
 */
@Component
class EmailVerificationRequestedListener {

    private final NotificationFacade notificationFacade;

    EmailVerificationRequestedListener(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(EmailVerificationRequestedEvent event) {
        notificationFacade.sendEmailVerificationOtp(new EmailOtpMessage(
                event.accountId(), event.email(), event.otp(), event.validFor()));
    }
}
