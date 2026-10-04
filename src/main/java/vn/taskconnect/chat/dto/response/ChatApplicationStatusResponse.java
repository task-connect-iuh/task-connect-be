package vn.taskconnect.chat.dto.response;

import java.util.UUID;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.api.TaskStatus;

/**
 * Trang thai cua 1 application + task cha, dung khi FE mo mot cuoc tro chuyen CHUA tung co kenh
 * chat vat ly (selectedItem rong trong Inbox vi channel chua duoc lazy-create, xem
 * InboxPage.tsx) de biet truoc co nen chan gui/hien thong bao "khong the bat dau hoi thoai"
 * truoc khi nguoi dung thu gui tin dau tien - vd task vua bi Admin tu choi hau kiem, hoac don da
 * WITHDRAWN/REJECTED tu truoc nhung chua ai tung nhan tin gi. Doc lap voi
 * ChatInboxItemResponse (khong doi hoi channelId/lastMessage vi kenh co the chua ton tai).
 */
public record ChatApplicationStatusResponse(
        UUID applicationId,
        UUID taskId,
        String taskTitle,
        UUID counterpartAccountId,
        String counterpartName,
        String counterpartAvatarUrl,
        String viewerRole,
        TaskApplicationStatus applicationStatus,
        TaskStatus taskStatus
) {
}
