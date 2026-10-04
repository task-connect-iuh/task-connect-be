package vn.taskconnect.chat.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.chat.api.ChatMessageType;
import vn.taskconnect.chat.api.ProposalStatus;
import vn.taskconnect.task.api.dto.ExtraCostBatchSummary;

/**
 * Mot tin nhan/de xuat nhin tu phia client, dung cho GET/POST .../messages va cung la payload
 * publish qua WebSocket toi "/topic/chat/{applicationId}" khi co tin nhan moi HOAC khi 1 tin
 * nhan da co thay doi (thu hoi/tha reaction/ghim - server publish lai DUNG message id, FE upsert
 * theo id, xem mergeMessage() trong InboxPage.tsx). priceProposalAmount chi co gia tri khi
 * messageType = PRICE_PROPOSAL (doc tu TaskFacade qua refPriceHistoryId, khong luu lai o
 * chat_messages de tranh 2 nguon su that - xem ChatMessage.priceProposal()).
 *
 * <p>Cac truong them 2026-09-26: {@code attachments} (rong neu khong phai IMAGE/FILE/VIDEO),
 * {@code replyToMessageId} (FE tu tra trong danh sach da nap de hien preview quote, khong lap
 * lai noi dung o day - toan bo lich su luon nap 1 lan, xem ChatService.listMessages Javadoc),
 * {@code recalledAt} (khac null = tin da bi thu hoi, khi do body/attachments/reactions duoi day
 * LUON rong/null bat ke du lieu that trong DB the nao - xem ChatService.toResponse),
 * {@code recallableUntil} (moc thoi gian con thu hoi duoc, de FE khong phai tu biet nguong 15
 * phut; null neu tin nay khong thuoc dien thu hoi duoc - khong phai TEXT/IMAGE/FILE/VIDEO, hoac
 * khong phai tin cua chinh nguoi dang xem), {@code reactions}, {@code pinnedAt}/
 * {@code pinnedByAccountId}/{@code pinnedByName} (null/rong neu tin khong duoc ghim).
 *
 * <p>{@code refTaskEditId} (them cho UC07): chi co khi day la SYSTEM message sinh ra tu 1 lan
 * Poster sua cong viec - FE dung id nay goi GET .../messages/{messageId}/task-edit de hien nut
 * "Xem chi tiet thay doi".
 *
 * <p>{@code refExtraCostBatchId}/{@code extraCostBatch} (them 2026-10-03): chi co gia tri khi
 * messageType = EXTRA_COST_BATCH (doc tu TaskFacade.findExtraCostBatch() qua
 * refExtraCostBatchId, khong luu lai o chat_messages de tranh 2 nguon su that - cung tinh than
 * voi priceProposalAmount o tren). FE dung extraCostBatch de ve the "Chi phi phat sinh" ngay
 * trong khung chat (giong PRICE_PROPOSAL), kem nut Dong y/Tu choi/Thu hoi goi thang API chi phi
 * phat sinh da co san.
 */
public record ChatMessageResponse(
        UUID id,
        UUID channelId,
        UUID senderAccountId,
        String senderName,
        String senderAvatarUrl,
        ChatMessageType messageType,
        String body,
        UUID refPriceHistoryId,
        Long priceProposalAmount,
        Instant proposedTime,
        ProposalStatus proposalStatus,
        Instant createdAt,
        List<ChatAttachmentResponse> attachments,
        UUID replyToMessageId,
        Instant recalledAt,
        Instant recallableUntil,
        List<ChatReactionResponse> reactions,
        Instant pinnedAt,
        UUID pinnedByAccountId,
        String pinnedByName,
        UUID refTaskEditId,
        UUID refExtraCostBatchId,
        ExtraCostBatchSummary extraCostBatch
) {
}
