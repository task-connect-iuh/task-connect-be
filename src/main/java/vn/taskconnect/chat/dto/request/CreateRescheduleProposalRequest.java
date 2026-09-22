package vn.taskconnect.chat.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Du lieu tao 1 de xuat doi lich lam viec (UC16 muc 9, Round B6), dung cho
 * POST /api/v1/chat/applications/{applicationId}/reschedule-proposals. proposedTime phai o
 * tuong lai - doi lich ve qua khu khong co y nghia nghiep vu.
 */
public record CreateRescheduleProposalRequest(
        @NotNull @Future Instant proposedTime,
        @Size(max = 2000) String note
) {
}
