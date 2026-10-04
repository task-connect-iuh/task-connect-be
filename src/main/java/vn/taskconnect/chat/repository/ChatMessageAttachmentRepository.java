package vn.taskconnect.chat.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.chat.entity.ChatMessageAttachment;

/**
 * Truy xuat du lieu bang chat_message_attachments. Chi module Chat duoc inject truc tiep
 * repository nay - module khac phai goi qua ChatFacade.
 */
public interface ChatMessageAttachmentRepository extends JpaRepository<ChatMessageAttachment, UUID> {

    /** Dinh kem cua 1 tin nhan, dung thu tu da chon luc gui. */
    List<ChatMessageAttachment> findByMessageIdOrderBySortOrderAsc(UUID messageId);

    /** Dinh kem cua nhieu tin nhan cung luc - dung khi liet ke ca kenh, tranh N+1 query. */
    List<ChatMessageAttachment> findByMessageIdInOrderBySortOrderAsc(Collection<UUID> messageIds);
}
