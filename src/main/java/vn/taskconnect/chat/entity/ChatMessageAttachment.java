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
 * Mot file dinh kem cua 1 tin nhan IMAGE/FILE/VIDEO - nhieu dong cho 1 messageId (gui nhieu anh
 * 1 luc). storageKey la S3 object key o prefix rieng tu "chat-attachments/" (khong public-read,
 * xem V41__add_chat_recall_reply_reactions_attachments_pins.sql va ADR-004) - KHONG luu URL
 * cong khai, moi lan tra ve client se duoc ky lai 1 presigned GET moi (xem
 * ChatService.toAttachmentResponse).
 */
@Entity
@Table(name = "chat_message_attachments")
public class ChatMessageAttachment {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "message_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID messageId;

    @Column(name = "storage_key", nullable = false, updatable = false, length = 500)
    private String storageKey;

    @Column(name = "file_name", nullable = false, updatable = false, length = 255)
    private String fileName;

    @Column(name = "mime_type", nullable = false, updatable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false, updatable = false)
    private long fileSizeBytes;

    @Column(name = "sort_order", nullable = false, updatable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessageAttachment() {
        // JPA
    }

    /** Tao 1 dong dinh kem moi cho 1 tin nhan da co id - khong tu sinh id tin nhan, chi tham chieu. */
    public static ChatMessageAttachment of(UUID id, UUID messageId, String storageKey, String fileName,
            String mimeType, long fileSizeBytes, int sortOrder, Instant now) {
        ChatMessageAttachment attachment = new ChatMessageAttachment();
        attachment.id = id;
        attachment.messageId = messageId;
        attachment.storageKey = storageKey;
        attachment.fileName = fileName;
        attachment.mimeType = mimeType;
        attachment.fileSizeBytes = fileSizeBytes;
        attachment.sortOrder = sortOrder;
        attachment.createdAt = now;
        return attachment;
    }

    /** Id noi bo cua dong dinh kem nay. */
    public UUID getId() {
        return id;
    }

    /** Id tin nhan chua dinh kem nay. */
    public UUID getMessageId() {
        return messageId;
    }

    /** S3 object key, prefix "chat-attachments/{applicationId}/". */
    public String getStorageKey() {
        return storageKey;
    }

    /** Ten file goc client gui len - chi de hien thi/tai xuong, khong dung de tra cuu. */
    public String getFileName() {
        return fileName;
    }

    /** Content type da xac thuc luc xin presigned URL. */
    public String getMimeType() {
        return mimeType;
    }

    /** Dung luong (byte) client bao cao luc gui - khong xac minh lai voi S3 that (cung han che voi ADR-003). */
    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    /** Thu tu hien thi trong 1 tin nhan nhieu file - theo dung thu tu nguoi dung da chon. */
    public int getSortOrder() {
        return sortOrder;
    }

    /** Thoi diem tao dong dinh kem - cung thoi diem voi tin nhan cha. */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
