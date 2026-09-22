package vn.taskconnect.chat.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.chat.api.ChatMessageType;
import vn.taskconnect.chat.api.ProposalStatus;
import vn.taskconnect.chat.entity.ChatMessage;

/**
 * Truy xuat du lieu bang chat_messages. Chi module Chat duoc inject truc tiep repository nay -
 * module khac phai goi qua ChatFacade.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /** Toan bo tin nhan cua 1 kenh, sap theo thoi gian gui - dung cho man lich su chat. */
    List<ChatMessage> findByChannelIdOrderByCreatedAtAsc(UUID channelId);

    /** Tin nhan gan nhat cua 1 kenh - dung de hien preview/thoi gian trong Inbox. */
    Optional<ChatMessage> findTopByChannelIdOrderByCreatedAtDesc(UUID channelId);

    /** Co de xuat dung loai dang PROPOSED trong kenh khong - dung cho ChatFacade.hasPendingProposal(), bat ke ai tao. */
    boolean existsByChannelIdAndMessageTypeAndProposalStatus(UUID channelId, ChatMessageType messageType,
            ProposalStatus proposalStatus);

    /**
     * Co de xuat dung loai dang PROPOSED trong kenh, tao boi nguoi KHAC accountId khong - dung
     * de tinh "Can phan hoi" trong Inbox (de xuat cua chinh minh khong tinh la can minh phan hoi).
     */
    boolean existsByChannelIdAndMessageTypeAndProposalStatusAndSenderAccountIdNot(UUID channelId,
            ChatMessageType messageType, ProposalStatus proposalStatus, UUID senderAccountId);

    /**
     * Tin nhan de xuat (PRICE_PROPOSAL) ung voi 1 tap task_price_history id - dung cho
     * ChatFacade.findProposalStatusesByPriceHistoryIds() (man "Lich su gia" o module Task).
     */
    List<ChatMessage> findByRefPriceHistoryIdIn(Collection<UUID> refPriceHistoryIds);
}
