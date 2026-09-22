package vn.taskconnect.task.api.dto;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.task.api.TaskApplicationStatus;

/**
 * Thong tin toi thieu 1 don ung tuyen de module khac (Chat) kiem tra quyen xem kenh, xac dinh
 * noi dung SYSTEM message mo dau, va hien thi Inbox - doc qua
 * {@link vn.taskconnect.task.api.TaskFacade#getApplicationParties(UUID)}. Ten/anh dai dien
 * Poster/Tasker do Task tu giai quyet qua UserFacade (Task da san co phu thuoc nay tu truoc),
 * de Chat khong can them phu thuoc rieng vao module User (tranh them 1 chieu goi moi).
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
        Instant createdAt
) {
}
