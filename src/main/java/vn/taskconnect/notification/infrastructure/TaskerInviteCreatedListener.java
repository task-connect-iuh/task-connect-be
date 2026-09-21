package vn.taskconnect.notification.infrastructure;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.taskconnect.auth.api.AuthFacade;
import vn.taskconnect.auth.api.dto.AccountSummary;
import vn.taskconnect.matching.api.event.TaskerInviteCreatedEvent;
import vn.taskconnect.notification.api.NotificationFacade;
import vn.taskconnect.notification.api.dto.TaskerInviteNotice;
import vn.taskconnect.task.api.TaskFacade;

/**
 * Cau noi giua su kien cua Matching (Poster vua moi mot Tasker) va viec gui mail cua
 * Notification - mirror EmailVerificationRequestedListener (module Auth). AFTER_COMMIT +
 * Async cung ly do: chi gui mail sau khi loi moi da chac chan nam trong DB, va SMTP khong
 * nam trong transaction nghiep vu (xem Javadoc EmailVerificationRequestedListener).
 *
 * <p>Can doc email that su qua AuthFacade va tieu de Task qua TaskFacade - TaskerInviteCreatedEvent
 * chi mang UUID (dung nguyen tac "khong dua du lieu dinh danh ca nhan vao payload su kien" cua
 * rule 13, khac voi EmailVerificationRequestedEvent phai mang email/otp vi do chinh la noi
 * dung can gui).
 */
@Component
class TaskerInviteCreatedListener {

    private final NotificationFacade notificationFacade;
    private final AuthFacade authFacade;
    private final TaskFacade taskFacade;

    TaskerInviteCreatedListener(NotificationFacade notificationFacade, AuthFacade authFacade, TaskFacade taskFacade) {
        this.notificationFacade = notificationFacade;
        this.authFacade = authFacade;
        this.taskFacade = taskFacade;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(TaskerInviteCreatedEvent event) {
        AccountSummary account = authFacade.findAccount(event.taskerId()).orElse(null);
        if (account == null) {
            return;
        }
        String taskTitle = taskFacade.findTask(event.taskId()).map(t -> t.title()).orElse("một công việc");
        notificationFacade.notifyTaskerInvited(new TaskerInviteNotice(event.taskerId(), account.email(), taskTitle));
    }
}
