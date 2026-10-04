package vn.taskconnect.chat.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Dung rieng cho ChatService.pinMessage(): khoa dong ngay luc doc (SELECT ... FOR UPDATE) de
     * chan race condition khi ca 2 ben (Poster/Tasker) cung ghim gan nhu dong thoi luc dang o
     * dung 3 tin ghim - cung mau voi KycVerificationRepository.findByIdForUpdate() (them
     * 2026-09-26). Khong dung cho cac thao tac khac (khong can khoa).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChatChannel c where c.id = :id")
    Optional<ChatChannel> findByIdForUpdate(@Param("id") UUID id);
}
