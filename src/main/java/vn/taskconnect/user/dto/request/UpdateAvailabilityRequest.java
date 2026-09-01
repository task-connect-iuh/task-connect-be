package vn.taskconnect.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;

/**
 * Sua mot khung gio ranh da khai bao, dung cho PATCH /users/me/tasker-availability/{id}
 * theo dung ngu nghia "sua mot phan" cua 16-api-contract.md: truong nao null giu nguyen
 * gia tri cu, khong truong nao bat buoc o muc DTO - khac CreateAvailabilityRequest (POST).
 */
public record UpdateAvailabilityRequest(
        @Min(1) @Max(7) Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
