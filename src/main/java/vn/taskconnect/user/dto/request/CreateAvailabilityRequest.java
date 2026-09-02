package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * Khai bao mot khung gio ranh moi, dung cho POST /users/me/tasker-availability. Ca ba
 * truong bat buoc (khac UpdateAvailabilityRequest dung cho PATCH, cho phep null tung phan).
 */
public record CreateAvailabilityRequest(
        // 1 = Thu 2 ... 7 = Chu nhat, dung khop CHECK constraint cua V2 migration.
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {
}
