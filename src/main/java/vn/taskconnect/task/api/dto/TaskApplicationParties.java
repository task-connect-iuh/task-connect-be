package vn.taskconnect.task.api.dto;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.api.TaskStatus;

/**
 * Thong tin toi thieu 1 don ung tuyen de module khac (Chat) kiem tra quyen xem kenh, xac dinh
 * noi dung SYSTEM message mo dau, va hien thi Inbox - doc qua
 * {@link vn.taskconnect.task.api.TaskFacade#getApplicationParties(UUID)}. Ten/anh dai dien
 * Poster/Tasker do Task tu giai quyet qua UserFacade (Task da san co phu thuoc nay tu truoc),
 * de Chat khong can them phu thuoc rieng vao module User (tranh them 1 chieu goi moi). taskStatus
 * them de Chat phan biet duoc "task cha vua bi Admin tu choi hau kiem" voi cac ly do dong kenh
 * khac von chi suy tu status cua CHINH don ung tuyen (status field o tren) - status don khong
 * doi khi Admin tu choi task (xem TaskService.rejectFlaggedTask()), can nguon rieng.
 */
public record TaskApplicationParties(
        UUID applicationId,
        UUID taskId,
        String taskTitle,
        UUID posterId,
        String posterName,
        String posterAvatarUrl,
        UUID taskerId,
        String taskerName,
        String taskerAvatarUrl,
        TaskApplicationStatus status,
        Instant createdAt,
        TaskStatus taskStatus
) {
}
