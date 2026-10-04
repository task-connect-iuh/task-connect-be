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
 * 1 emoji ma 1 tai khoan da tha vao 1 tin nhan - toi da 1 dong/(messageId, accountId) qua UNIQUE
 * trong V41. Tha lai emoji khac GHI DE dong cu, tha lai CUNG emoji XOA dong (bo tha) - xu ly o
 * ChatService.reactToMessage(), entity chi luu trang thai hien tai.
 */
@Entity
@Table(name = "chat_message_reactions")
public class ChatMessageReaction {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "message_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID messageId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "emoji", nullable = false, length = 32)
    private String emoji;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChatMessageReaction() {
        // JPA
    }

    /** Tao 1 reaction moi cua 1 tai khoan tren 1 tin nhan. */
    public static ChatMessageReaction of(UUID id, UUID messageId, UUID accountId, String emoji, Instant now) {
        ChatMessageReaction reaction = new ChatMessageReaction();
        reaction.id = id;
        reaction.messageId = messageId;
        reaction.accountId = accountId;
        reaction.emoji = emoji;
        reaction.createdAt = now;
        reaction.updatedAt = now;
        return reaction;
    }

    /** Doi sang emoji khac (nguoi dung tha lai emoji khac tren cung tin nhan). */
    public void changeEmoji(String emoji, Instant now) {
        this.emoji = emoji;
        this.updatedAt = now;
    }

    /** Id noi bo cua dong reaction nay. */
    public UUID getId() {
        return id;
    }

    /** Id tin nhan duoc tha reaction. */
    public UUID getMessageId() {
        return messageId;
    }

    /** Id tai khoan da tha reaction nay. */
    public UUID getAccountId() {
        return accountId;
    }

    /** Ky tu emoji hien tai. */
    public String getEmoji() {
        return emoji;
    }

    /** Thoi diem tha lan dau. */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
