package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 1 khoan trong form "Chi phi phat sinh" Tasker dang - ten khoan va so tien bat buoc, anh tuy
 * chon (publicUrl tra ve tu POST .../extra-cost-images/upload-url). Nguong 10.000 d - 10.000.000
 * d moi khoan chot cung nguoi dung 2026-10-03, cung kieu @Min/@Max voi budgetAmount tren
 * CreateTaskRequest.java.
 */
public record ExtraCostItemInput(
        @NotBlank String name,
        @Min(value = 10_000, message = "Số tiền mỗi khoản phải từ 10.000 đ trở lên.")
        @Max(value = 10_000_000, message = "Số tiền mỗi khoản không được vượt quá 10.000.000 đ.")
        long amount,
        String photoUrl) {
}
