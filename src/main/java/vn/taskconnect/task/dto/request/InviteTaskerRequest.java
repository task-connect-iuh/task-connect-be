package vn.taskconnect.task.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Poster moi truc tiep mot Tasker nhan cong viec (UC09, Round B5), dung cho
 * POST /api/v1/tasks/{taskId}/invitations. proposedPrice/message deu tuy chon - neu co
 * proposedPrice, no duoc gui vao kenh chat nhu 1 PRICE_PROPOSAL binh thuong (khong luu thang
 * vao task_applications.proposed_price, xem TaskApplication.invite()).
 */
public record InviteTaskerRequest(
        @NotNull UUID taskerId,
        @Positive Long proposedPrice,
        @Size(max = 1000) String message
) {
}
