package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Du lieu dang mot cong viec moi, dung cho POST /api/v1/tasks. lat/lng KHONG bat buoc o muc
 * Bean Validation ({@code @NotNull}) du la truong bat buoc nghiep vu - kiem tra rieng o
 * TaskService de nem dung TSK-400-MISSING_LOCATION (ma da reserve san trong ErrorCode) thay
 * vi loi validation chung chung. budgetAmount/scheduledAt tuy chon theo quyet dinh da chot
 * voi nguoi dung. estimatedWorkersNeeded tuy chon - FE dot nay khong con o nhap, luon gui 1;
 * de null van duoc, TaskService tu ap mac dinh.
 */
public record CreateTaskRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 150) String title,
        @NotBlank String description,
        @NotBlank @Size(max = 500) String addressText,
        BigDecimal lat,
        BigDecimal lng,
        @Positive Long budgetAmount,
        Instant scheduledAt,
        Integer estimatedWorkersNeeded,
        @Size(max = 5) List<@NotBlank String> imageUrls
) {
}
