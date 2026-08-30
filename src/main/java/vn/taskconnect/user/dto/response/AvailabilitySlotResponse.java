package vn.taskconnect.user.dto.response;

import java.time.LocalTime;
import java.util.UUID;
import vn.taskconnect.user.entity.TaskerAvailability;

/** Mot khung gio ranh, tra ve cho client (POST/GET/PATCH /users/me/tasker-availability). */
public record AvailabilitySlotResponse(UUID id, int dayOfWeek, LocalTime startTime, LocalTime endTime) {

    /** Chuyen entity sang DTO tra ve qua API, tranh entity JPA lo ra ngoai lop controller. */
    public static AvailabilitySlotResponse from(TaskerAvailability slot) {
        return new AvailabilitySlotResponse(slot.getId(), slot.getDayOfWeek(), slot.getStartTime(),
                slot.getEndTime());
    }
}
