package vn.taskconnect.chat.service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.taskconnect.chat.api.ChatFacade;
import vn.taskconnect.chat.api.ChatMessageType;
import vn.taskconnect.chat.api.ProposalStatus;

/**
 * Trien khai duy nhat cua ChatFacade - khong module nao khac trong chat duoc implements
 * interface nay. Uy quyen toan bo cho ChatService (cung module, cung package) - giu ChatFacade
 * la be mat mong, logic that nam o ChatService de ChatController (dung intra-module) khong
 * phai di vong qua Facade cua chinh module minh.
 */
@Service
class ChatFacadeImpl implements ChatFacade {

    private final ChatService chatService;

    ChatFacadeImpl(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void openChannelWithFirstInquiryMessage(UUID applicationId, UUID taskerAccountId,
            String systemMessageBody, String inquiryMessageText, Instant createdAt) {
        chatService.openChannelWithFirstInquiryMessage(applicationId, taskerAccountId, systemMessageBody,
                inquiryMessageText, createdAt);
    }

    @Override
    public void closeChannelIfExists(UUID applicationId, String systemMessageBody) {
        chatService.closeChannelIfExists(applicationId, systemMessageBody);
    }

    @Override
    public void postSystemMessageIfOpen(UUID applicationId, String systemMessageBody) {
        chatService.postSystemMessageIfOpen(applicationId, systemMessageBody);
    }

    @Override
    public boolean hasPendingProposal(UUID applicationId, ChatMessageType type) {
        return chatService.hasPendingProposal(applicationId, type);
    }

    @Override
    public Map<UUID, ProposalStatus> findProposalStatusesByPriceHistoryIds(Set<UUID> priceHistoryIds) {
        return chatService.findProposalStatusesByPriceHistoryIds(priceHistoryIds);
    }

    @Override
    public void attachBooking(UUID applicationId, UUID bookingId) {
        chatService.attachBooking(applicationId, bookingId);
    }

    @Override
    public void notifyApplicationConfirmed(UUID applicationId, String systemMessageBody, Instant now) {
        chatService.notifyApplicationConfirmed(applicationId, systemMessageBody, now);
    }

    @Override
    public void openChannelForInvite(UUID applicationId, UUID posterAccountId, String systemMessageBody,
            Long proposedAmount, String proposalNote, Instant now) {
        chatService.openChannelForInvite(applicationId, posterAccountId, systemMessageBody, proposedAmount,
                proposalNote, now);
    }
}
