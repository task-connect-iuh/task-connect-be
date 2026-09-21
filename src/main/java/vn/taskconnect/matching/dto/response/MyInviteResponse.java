package vn.taskconnect.matching.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.matching.api.TaskerInviteStatus;
import vn.taskconnect.task.api.TaskStatus;

/**
 * Rut gon Task + trang thai loi moi cho man "Loi moi ban nhan duoc" (Tasker-side) - mirror
 * MyApplicationResponse.java cua module Task (xem TaskApplicationService.listMyApplications()),
 * nhung nguon du lieu la matching_tasker_invites thay vi task_applications. taskImageUrls luon
 * rong o vong nay - TaskFacade chua lo anh Task, khong dang them phu thuoc facade moi chi cho
 * man hinh phu nay (xem plan da duyet, muc Frontend). taskLat/taskLng dung cho tinh nang "Chi
 * duong" (VietMap routing) o FE - xem DirectionsModal.tsx.
 */
public record MyInviteResponse(
        UUID inviteId,
        TaskerInviteStatus status,
        Instant createdAt,
        Instant respondedAt,
        UUID taskId,
        String taskTitle,
        String taskAddressText,
        BigDecimal taskLat,
        BigDecimal taskLng,
        Long taskBudgetAmount,
        Instant taskScheduledAt,
        TaskStatus taskStatus,
        UUID categoryId,
        String categoryName,
        String posterName,
        List<String> taskImageUrls
) {
}
