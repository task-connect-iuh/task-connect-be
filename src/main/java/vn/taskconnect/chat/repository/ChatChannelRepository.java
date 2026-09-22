package vn.taskconnect.chat.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.taskconnect.chat.entity.ChatChannel;

/**
 * Truy xuat du lieu bang chat_channels. Chi module Chat duoc inject truc tiep repository nay -
 * module khac phai goi qua ChatFacade.
 */
public interface ChatChannelRepository extends JpaRepository<ChatChannel, UUID> {

    /** Kenh gan voi 1 application - UNIQUE trong V32, toi da 1 ket qua. */
    Optional<ChatChannel> findByApplicationId(UUID applicationId);

    /** Toan bo kenh cua tap applicationId cho truoc - dung cho Inbox (danh sach applicationId lay tu TaskFacade). */
    List<ChatChannel> findByApplicationIdIn(Collection<UUID> applicationIds);
}
