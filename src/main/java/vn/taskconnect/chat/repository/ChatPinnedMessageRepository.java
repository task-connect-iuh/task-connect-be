package vn.taskconnect.chat.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.chat.entity.ChatPinnedMessage;

/**
 * Truy xuat du lieu bang chat_pinned_messages. Chi module Chat duoc inject truc tiep repository
 * nay - module khac phai goi qua ChatFacade. Gioi han toi da 3 dong/channelId KHONG nam o day
 * (khong the dien ta bang query) - xem ChatService.pinMessage().
 */
public interface ChatPinnedMessageRepository extends JpaRepository<ChatPinnedMessage, UUID> {

    /** Toan bo tin dang ghim cua 1 kenh, cu nhat truoc - dung de hien thanh ghim va dem gioi han. */
    List<ChatPinnedMessage> findByChannelIdOrderByCreatedAtAsc(UUID channelId);

    /** Toan bo tin dang ghim cua nhieu kenh - dung khi liet ke Inbox/lich su, tranh N+1 query. */
    List<ChatPinnedMessage> findByChannelIdIn(Collection<UUID> channelIds);

    /** So tin dang ghim hien tai cua 1 kenh - dung de kiem tra gioi han truoc khi ghim them. */
    long countByChannelId(UUID channelId);

    /** Xoa 1 dong ghim theo dung channel+message - dung cho unpinMessage(), no-op neu chua tung ghim. */
    void deleteByChannelIdAndMessageId(UUID channelId, UUID messageId);

    /** Xoa dong ghim (neu co) cua 1 tin nhan - dung khi tin bi thu hoi (noi dung khong con, tu dong bo ghim). */
    void deleteByMessageId(UUID messageId);
}
