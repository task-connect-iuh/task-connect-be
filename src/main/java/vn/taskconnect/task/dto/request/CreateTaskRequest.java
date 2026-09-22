package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.task.api.SuppliesStatus;
import vn.taskconnect.user.api.LocationType;

/**
 * Du lieu dang mot cong viec moi, dung cho POST /api/v1/tasks. lat/lng KHONG bat buoc o muc
 * Bean Validation ({@code @NotNull}) du la truong bat buoc nghiep vu - kiem tra rieng o
 * TaskService de nem dung TSK-400-MISSING_LOCATION (ma da reserve san trong ErrorCode) thay
 * vi loi validation chung chung. budgetAmount/scheduledAt tuy chon theo quyet dinh da chot
 * voi nguoi dung - nhung khi CO nhap thi budgetAmount phai nam trong 100.000 d - 50.000.000 d
 * (nguong nghiep vu chot cung voi nguoi dung 2026-09-16, khop BUDGET_MIN_VND/BUDGET_MAX_VND o
 * PostTaskPage.tsx). estimatedWorkersNeeded tuy chon - FE dot nay khong con o nhap, luon gui 1;
 * de null van duoc, TaskService tu ap mac dinh. locationType/arrivalNotes tuy chon - FE dien
 * san tu ho so Poster luc mo form (xem V24__add_location_type_arrival_notes_to_task_tasks.sql),
 * nguoi dung sua duoc truoc khi dang, khong bat buoc. suppliesStatus bat buoc (khong co gia
 * tri mac dinh o FE, Poster phai tu chon). suppliesNote LUON tuy chon o muc validate, ke ca
 * khi suppliesStatus la FULL hoac PARTIAL - xem V26__add_supplies_fields_to_task_tasks.sql.
 */
public record CreateTaskRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 150) String title,
        @NotBlank String description,
        @NotBlank @Size(max = 500) String addressText,
        BigDecimal lat,
        BigDecimal lng,
        LocationType locationType,
        @Size(max = 500) String arrivalNotes,
        @NotNull SuppliesStatus suppliesStatus,
        String suppliesNote,
        // Don vi: dong (VND) nguyen. null = "thoa thuan", van hop le.
        @Min(value = 100_000, message = "Ngân sách phải từ 100.000 đ trở lên.")
        @Max(value = 50_000_000, message = "Ngân sách không được vượt quá 50.000.000 đ.")
        Long budgetAmount,
        Instant scheduledAt,
        Integer estimatedWorkersNeeded,
        @Size(max = 5) List<@NotBlank String> imageUrls
) {
}
