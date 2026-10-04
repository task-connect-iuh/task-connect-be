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
 * (VietMap routing) o FE - xem DirectionsModal.tsx. agreedPriceAmount them (2026-09-28) de FE
 * hien "Ban nhan duoc" (sau khi tru phi nen tang) dung gia THAT da chot qua chat neu co, thay vi
 * chi uoc tinh tu taskBudgetAmount - cung cach tinh voi TaskApplicationResponse.agreedPriceAmount
 * ben phia Poster (xem TaskApplicationService.priceSnapshotFor). hasBooking them cung ngay
 * (2026-09-28) - don UNG VIEN THANG cua UC11 "Chon nguoi nay" GIU NGUYEN status=PENDING (khong
 * chuyen ACCEPTED, xem Javadoc TaskApplicationService.confirm()), nen FE khong the dua vao status
 * de biet da duoc chon hay chua; hasBooking (tu BookingFacade.findByApplicationId) moi la tin
 * hieu dung, dung de FE chuyen the cong viec sang tab "Da nhan" thay vi "Cho xac nhan".
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
        List<String> taskImageUrls,
        Long agreedPriceAmount,
        boolean hasBooking
) {
}
