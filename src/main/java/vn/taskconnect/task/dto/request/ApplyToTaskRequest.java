package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Du lieu Tasker gui khi ung tuyen mot cong viec, dung cho POST /api/v1/tasks/{taskId}/applications.
 * proposedArrivalText da bo khoi form (yeu cau nguoi dung), gio la tuy chon - khong con @NotBlank,
 * FE hien tai khong con gui truong nay. proposedPrice/priceReason them lai 2026-09-30 (yeu cau
 * nguoi dung) cho lua chon "De nghi mot muc khac" o form ung tuyen: neu co proposedPrice thi
 * priceReason BAT BUOC khong duoc rong - rang buoc CHEO 2 truong nay kiem tra o
 * TaskApplicationService.apply() (khong the khai bao bang Bean Validation don gian). proposedPrice
 * dung CHUNG bien do voi Task.budgetAmount (100.000 d - 50.000.000 d, xem CreateTaskRequest) theo
 * yeu cau nguoi dung - FE (TaskerJobDetailPage.tsx) nhap don vi nghin dong roi nhan 1000 truoc
 * khi gui, khop dung don vi dong nguyen o day. Ca 2 di qua kenh chat nhu 1 PRICE_PROPOSAL binh
 * thuong, giong het InviteTaskerRequest.proposedPrice() - khong luu thang vao
 * task_applications.proposed_price (cot do chi phan anh gia CA HAI BEN da dong y, xem
 * TaskApplication.updateProposedPrice()).
 */
public record ApplyToTaskRequest(
        @Size(max = 200) String proposedArrivalText,
        @Size(max = 2000) String message,
        @Min(value = 100_000, message = "Mức đề nghị phải từ 100.000 đ trở lên.")
        @Max(value = 50_000_000, message = "Mức đề nghị không được vượt quá 50.000.000 đ.")
        Long proposedPrice,
        @Size(max = 1000) String priceReason
) {
}
