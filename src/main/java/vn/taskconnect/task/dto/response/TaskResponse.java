package vn.taskconnect.task.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.task.api.SuppliesStatus;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.user.api.LocationType;

/**
 * Phan hoi day du mot cong viec, dung chung cho POST /tasks, GET /tasks/mine, GET /tasks/{id}.
 */
public record TaskResponse(
        UUID id,
        UUID posterId,
        UUID categoryId,
        String categoryName,
        String title,
        String description,
        String addressText,
        BigDecimal lat,
        BigDecimal lng,
        LocationType locationType,
        String arrivalNotes,
        SuppliesStatus suppliesStatus,
        String suppliesNote,
        Long budgetAmount,
        Instant scheduledAt,
        int estimatedWorkersNeeded,
        TaskStatus status,
        List<String> imageUrls,
        Instant createdAt,
        Instant updatedAt,
        // So don ung tuyen dang PENDING (cho Poster xac nhan/tu choi) cua task nay - dung o FE de
        // loc tab "Can xu ly" trong Viec cua toi (chi tinh cho task OPEN, xem TaskService.getMyTasks).
        int pendingApplicantCount
) {

    /** Anh xa tu entity, ten danh muc (da tra tu UserFacade) va danh sach URL anh sang DTO tra ve FE. */
    public static TaskResponse from(Task task, String categoryName, List<String> imageUrls, int pendingApplicantCount) {
        return new TaskResponse(task.getId(), task.getPosterId(), task.getCategoryId(), categoryName,
                task.getTitle(), task.getDescription(), task.getAddressText(), task.getLat(), task.getLng(),
                task.getLocationType(), task.getArrivalNotes(), task.getSuppliesStatus(), task.getSuppliesNote(),
                task.getBudgetAmount(), task.getScheduledAt(), task.getEstimatedWorkersNeeded(), task.getStatus(),
                imageUrls, task.getCreatedAt(), task.getUpdatedAt(), pendingApplicantCount);
    }
}
