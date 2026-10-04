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
        // Ly do admin tu choi (task_tasks.rejection_reason) - null tru khi status = REJECTED, dung o
        // FE de Poster xem vi sao cong viec bi tu choi (xem TaskController.rejectFlaggedTask).
        String rejectionReason,
        List<String> imageUrls,
        Instant createdAt,
        Instant updatedAt,
        // So don ung tuyen dang PENDING (cho Poster xac nhan/tu choi) cua task nay - dung o FE de
        // loc tab "Can xu ly" trong Viec cua toi (chi tinh cho task OPEN, xem TaskService.getMyTasks).
        int pendingApplicantCount,
        // Ngan sach (Tang 2, UC07) co dang bi khoa hay khong - true khi con BAT KY don
        // PENDING/INQUIRING/INVITED/TIME_CHANGED_NEEDS_RECONFIRM nao (xem TaskService.
        // ACTIVE_STATUSES_ON_TASK_CANCEL), RONG hon pendingApplicantCount o tren (chi dem PENDING,
        // dung rieng cho tab "Can xu ly"/thong bao huy) - yeu cau nguoi dung 2026-09-30: 1 don dang
        // hoi them hoac duoc moi cung du de khoa, khong can doi den luc thanh don ung tuyen PENDING.
        boolean budgetLocked,
        // Vat tu (suppliesStatus/suppliesNote, Tang 2, UC07) co dang bi khoa hay khong - HEP hon
        // budgetLocked o tren: CHI true khi co don PENDING/TIME_CHANGED_NEEDS_RECONFIRM (xem
        // TaskService.PENDING_LIKE_STATUSES) - INQUIRING/INVITED KHONG khoa vat tu, van sua duoc
        // (yeu cau nguoi dung 2026-09-30, vong 2: vat tu it nhay cam hon ngan sach).
        boolean suppliesLocked,
        // Id application da duoc chon o UC11 cho task nay, null neu task chua ASSIGNED (xem
        // TaskService.resolveWinningApplicationId) - them 2026-10-02 de FE (TaskDetailDialog) co
        // cach lay applicationId ma goi API tong hop "Chi phi phat sinh"
        // (GET /tasks/applications/{applicationId}/extra-costs/summary), khong phai tu suy doan.
        UUID winningApplicationId
) {

    /** Anh xa tu entity, ten danh muc (da tra tu UserFacade) va danh sach URL anh sang DTO tra ve FE. */
    public static TaskResponse from(Task task, String categoryName, List<String> imageUrls,
            int pendingApplicantCount, boolean budgetLocked, boolean suppliesLocked, UUID winningApplicationId) {
        return new TaskResponse(task.getId(), task.getPosterId(), task.getCategoryId(), categoryName,
                task.getTitle(), task.getDescription(), task.getAddressText(), task.getLat(), task.getLng(),
                task.getLocationType(), task.getArrivalNotes(), task.getSuppliesStatus(), task.getSuppliesNote(),
                task.getBudgetAmount(), task.getScheduledAt(), task.getEstimatedWorkersNeeded(), task.getStatus(),
                task.getRejectionReason(), imageUrls, task.getCreatedAt(), task.getUpdatedAt(), pendingApplicantCount,
                budgetLocked, suppliesLocked, winningApplicationId);
    }
}
