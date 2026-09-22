package vn.taskconnect.task.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.task.api.SuppliesStatus;
import vn.taskconnect.user.api.LocationType;

/**
 * Mot cong viec dang OPEN hien trong feed cho Tasker duyet/ung tuyen (UC10), dung cho
 * GET /api/v1/tasks va GET /api/v1/tasks/{taskId}/browse. KHONG co distanceKm (can vi tri
 * Tasker + ban kinh mac dinh, OQ-02 con MO trong docs/OPEN-QUESTIONS.md) va KHONG co diem uy
 * tin/so luot danh gia cua Poster (module Review chua ton tai) - hai truong nay bo hoan toan
 * thay vi bia gia tri, xem docs/PROGRESS-TASK-TASKER-MODULE.md phan bao cao gap. locationType/
 * arrivalNotes/suppliesStatus/suppliesNote them vao (2026-09-14) de Tasker xem duoc dung
 * thong tin Poster da khai bao luc dang viec, xem TaskResponse (DTO tuong duong ben Poster).
 */
public record TaskFeedItemResponse(
        UUID id,
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
        List<String> imageUrls,
        UUID posterId,
        String posterName,
        String posterAvatarUrl,
        Instant createdAt
) {
}
