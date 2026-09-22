package vn.taskconnect.task.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.task.api.SuppliesStatus;
import vn.taskconnect.task.api.TaskApplicationInitiator;
import vn.taskconnect.task.api.TaskApplicationStatus;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.user.api.LocationType;

/**
 * Mot don ung tuyen nhin tu phia Tasker (man "Viec da nhan"), dung cho
 * GET /api/v1/tasks/applications/mine. Gom ca thong tin toi thieu cua cong viec de FE khong
 * phai goi them request rieng cho tung dong. taskDescription/taskLocationType/
 * taskArrivalNotes/taskSuppliesStatus/taskSuppliesNote them vao (2026-09-14) de FE dung cho
 * nut "Xem chi tiet" o TaskerJobsPage.tsx, cung du lieu voi TaskResponse/TaskFeedItemResponse
 * ben Poster/feed. initiatedBy/expiresAt them tu Round B5 de FE dung cho tab "Loi moi"
 * (INVITED, initiatedBy=POSTER) kem dem nguoc het han.
 * phai goi them request rieng cho tung dong. taskLat/taskLng dung cho tinh nang "Chi duong"
 * (VietMap routing) o FE - xem DirectionsModal.tsx.
 */
public record MyApplicationResponse(
        UUID applicationId,
        TaskApplicationStatus status,
        TaskApplicationInitiator initiatedBy,
        Instant expiresAt,
        String proposedArrivalText,
        String message,
        Instant createdAt,
        Instant respondedAt,
        UUID taskId,
        String taskTitle,
        String taskDescription,
        String taskAddressText,
        LocationType taskLocationType,
        String taskArrivalNotes,
        SuppliesStatus taskSuppliesStatus,
        String taskSuppliesNote,
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
