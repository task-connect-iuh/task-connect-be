package vn.taskconnect.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 1 tin nhan dang duoc ghim trong 1 kenh - toi da 3 dong/channelId (gioi han doc tu
 * admin_system_parameters, thuc thi o ChatService.pinMessage() qua khoa PESSIMISTIC_WRITE tren
 * ChatChannel, khong dien ta duoc bang CHECK/UNIQUE). Ca hai ben (Poster/Tasker) deu ghim/bo ghim
 * duoc bat ky tin nao trong danh sach chung nay (khong phai so huu rieng tung nguoi).
 */
@Entity
@Table(name = "chat_pinned_messages")
public class ChatPinnedMessage {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "channel_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID channelId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "message_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID messageId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "pinned_by_account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID pinnedByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatPinnedMessage() {
        // JPA
    }

    /** Ghim 1 tin nhan vao 1 kenh - goi sau khi ChatService da kiem tra chua vuot qua 3. */
    public static ChatPinnedMessage of(UUID id, UUID channelId, UUID messageId, UUID pinnedByAccountId, Instant now) {
        ChatPinnedMessage pinned = new ChatPinnedMessage();
        pinned.id = id;
        pinned.channelId = channelId;
        pinned.messageId = messageId;
        pinned.pinnedByAccountId = pinnedByAccountId;
        pinned.createdAt = now;
        return pinned;
    }

    /** Id noi bo cua dong ghim nay. */
    public UUID getId() {
        return id;
    }

    /** Id kenh chua tin nhan duoc ghim. */
    public UUID getChannelId() {
        return channelId;
    }

    /** Id tin nhan dang duoc ghim. */
    public UUID getMessageId() {
        return messageId;
    }

    /** Id tai khoan da thuc hien ghim - co the la Poster hoac Tasker, ca hai deu ghim/bo ghim duoc. */
    public UUID getPinnedByAccountId() {
        return pinnedByAccountId;
    }

    /** Thoi diem ghim. */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
