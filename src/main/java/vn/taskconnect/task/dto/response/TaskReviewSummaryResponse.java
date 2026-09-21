package vn.taskconnect.task.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.task.api.TaskAiFlagReason;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.task.entity.Task;
import vn.taskconnect.user.api.LocationType;

/**
 * Mot dong trong hang doi hau kiem cua Admin (xem .claude/rules/15-ai-module.md) - chi cac
 * cong viec dang needs_admin_review = true. Tra day du thong tin cong viec (khong chi mo ta)
 * de Admin xem toan bo chi tiet ngay trong hang doi, khong can goi API rieng - danh sach nay
 * von da it ban ghi (chi cong viec bi gan co) nen khong lo tai trong.
 */
public record TaskReviewSummaryResponse(
        UUID id,
        String title,
        String description,
        UUID categoryId,
        String categoryName,
        String addressText,
        BigDecimal lat,
        BigDecimal lng,
        LocationType locationType,
        String arrivalNotes,
        Long budgetAmount,
        Instant scheduledAt,
        int estimatedWorkersNeeded,
        List<String> imageUrls,
        TaskAiFlagReason aiFlagReason,
        UUID posterId,
        TaskStatus status,
        Instant createdAt
) {

    /** Anh xa tu entity Task + ten danh muc/danh sach anh da doc san o TaskService, tranh N+1 query. */
    public static TaskReviewSummaryResponse from(Task task, String categoryName, List<String> imageUrls) {
        return new TaskReviewSummaryResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCategoryId(),
                categoryName,
                task.getAddressText(),
                task.getLat(),
                task.getLng(),
                task.getLocationType(),
                task.getArrivalNotes(),
                task.getBudgetAmount(),
                task.getScheduledAt(),
                task.getEstimatedWorkersNeeded(),
                imageUrls,
                task.getAiFlagReason(),
                task.getPosterId(),
                task.getStatus(),
                task.getCreatedAt());
    }
}
