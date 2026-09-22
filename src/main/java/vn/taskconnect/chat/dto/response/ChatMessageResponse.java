package vn.taskconnect.chat.dto.response;

import java.time.Instant;
import java.util.UUID;
import vn.taskconnect.chat.api.ChatMessageType;
import vn.taskconnect.chat.api.ProposalStatus;

/**
 * Mot tin nhan/de xuat nhin tu phia client, dung cho GET/POST .../messages va cung la payload
 * publish qua WebSocket toi "/topic/chat/{applicationId}" khi co tin nhan moi. priceProposalAmount
 * chi co gia tri khi messageType = PRICE_PROPOSAL (doc tu TaskFacade qua refPriceHistoryId,
 * khong luu lai o chat_messages de tranh 2 nguon su that - xem ChatMessage.priceProposal()).
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
        Instant createdAt
) {
}
