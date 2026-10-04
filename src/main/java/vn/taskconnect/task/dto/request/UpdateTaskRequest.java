package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import vn.taskconnect.task.api.SuppliesStatus;
import vn.taskconnect.user.api.LocationType;

/**
 * Du lieu Poster sua mot cong viec da dang (UC07), dung cho PATCH /api/v1/tasks/{taskId}. Day
 * la GHI DE TOAN BO nhom truong sua duoc - null nghia la "xoa gia tri" (vd budgetAmount = null
 * la "thoa thuan"), khong phai "khong doi". Client luon gui du 6 truong, ke ca cac truong Tang
 * 2 dang bi disable tren form (TaskService tu so sanh gia tri moi voi gia tri hien tai de biet
 * truong nao THAT SU doi, xem Javadoc TaskService.updateTask). title/description/anh/
 * addressText/lat/lng/categoryId/estimatedWorkersNeeded KHOA CUNG VINH VIEN - khong nam trong
 * request nay, khong sua duoc o bat ky giai doan nao (giu nguyen rule UC07 goc). Cac nguong
 * @Min/@Max copy tu CreateTaskRequest.budgetAmount, khong duoc lech.
 */
public record UpdateTaskRequest(
        LocationType locationType,
        @Size(max = 500) String arrivalNotes,
        @NotNull SuppliesStatus suppliesStatus,
        String suppliesNote,
        @Min(value = 100_000, message = "Ngân sách phải từ 100.000 đ trở lên.")
        @Max(value = 50_000_000, message = "Ngân sách không được vượt quá 50.000.000 đ.")
        Long budgetAmount,
        Instant scheduledAt
) {
}
