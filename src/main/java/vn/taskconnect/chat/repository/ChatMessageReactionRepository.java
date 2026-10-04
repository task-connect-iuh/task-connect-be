package vn.taskconnect.chat.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.chat.entity.ChatMessageReaction;

/**
 * Truy xuat du lieu bang chat_message_reactions. Chi module Chat duoc inject truc tiep
 * repository nay - module khac phai goi qua ChatFacade.
 */
public interface ChatMessageReactionRepository extends JpaRepository<ChatMessageReaction, UUID> {

    /** Reaction cua 1 tai khoan tren 1 tin nhan - toi da 1 dong (UNIQUE), dung de kiem tra toggle. */
    Optional<ChatMessageReaction> findByMessageIdAndAccountId(UUID messageId, UUID accountId);

    /** Toan bo reaction cua nhieu tin nhan cung luc - dung khi liet ke ca kenh, tranh N+1 query. */
    List<ChatMessageReaction> findByMessageIdIn(Collection<UUID> messageIds);

    /** Xoa toan bo reaction cua 1 tin nhan - dung khi tin bi thu hoi (noi dung khong con). */
    void deleteByMessageId(UUID messageId);
}
