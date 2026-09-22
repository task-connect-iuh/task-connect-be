package vn.taskconnect.task.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.chat.api.ProposalStatus;
import vn.taskconnect.task.api.ChangeType;

/**
 * Mot dong lich su de xuat gia (ban ghi CHI GHI THEM cua task_price_history), dung cho
 * GET /api/v1/tasks/applications/{applicationId}/price-history (man "Lich su gia"). acceptedAt
 * null KHONG con dong nghia "chua xu ly" tu 2026-09-21 - dung them field status (doc tu
 * chat_messages.proposal_status qua ChatFacade.findProposalStatusesByPriceHistoryIds(), xem
 * Javadoc TaskApplicationService.toPriceHistoryEntryResponse()) de phan biet dung PROPOSED (con
 * dang cho quyet dinh) voi REJECTED (da bi Tu choi hoac chinh nguoi de xuat Thu hoi - ProposalStatus
 * khong tach 2 truong hop nay). status luon di kem, KHONG con la truong hop "chi suy tu acceptedAt"
 * nhu truoc.
 */
public record TaskPriceHistoryEntryResponse(
        UUID id,
        ChangeType changeType,
        long amount,
        String note,
        UUID createdByAccountId,
        String createdByName,
        Instant createdAt,
        UUID acceptedByAccountId,
        String acceptedByName,
        Instant acceptedAt,
        ProposalStatus status
) {
}
