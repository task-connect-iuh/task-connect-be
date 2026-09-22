package vn.taskconnect.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.taskconnect.chat.api.ChannelStatus;

/**
 * Kenh chat cua 1 application (toi da 1 channel/application, UNIQUE trong V32). Xem
 * TaskConnect_Chat_ImplementationSpec.md muc 1, 2 va 6.
 */
@Entity
@Table(name = "chat_channels")
public class ChatChannel {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "application_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID applicationId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "booking_id", columnDefinition = "BINARY(16)")
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ChannelStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatChannel() {
        // JPA
    }

    /** Tao 1 kenh moi o trang thai OPEN - lazy-create dung luc tin nhan/de xuat dau tien duoc gui thanh cong (dac ta muc 2). */
    public static ChatChannel open(UUID id, UUID applicationId, Instant now) {
        ChatChannel channel = new ChatChannel();
        channel.id = id;
        channel.applicationId = applicationId;
        channel.status = ChannelStatus.OPEN;
        channel.createdAt = now;
        return channel;
    }

    /** Dong kenh - khong gui/nhan them duoc tin nhan/de xuat moi sau khi dong (dac ta muc 6). */
    public void close() {
        this.status = ChannelStatus.CLOSED;
    }

    /** Gan booking vao kenh sau khi application duoc chon o UC11 (Round B4). */
    public void attachBooking(UUID bookingId) {
        this.bookingId = bookingId;
    }

    /** Id noi bo cua kenh nay. */
    public UUID getId() {
        return id;
    }

    /** Id application ma kenh nay thuoc ve - 1-1 (UNIQUE trong V32). */
    public UUID getApplicationId() {
        return applicationId;
    }

    /** Id booking duoc gan vao kenh nay o UC11, null neu application chua duoc chon. */
    public UUID getBookingId() {
        return bookingId;
    }

    /** Trang thai hien tai cua kenh. */
    public ChannelStatus getStatus() {
        return status;
    }

    /**
     * Thoi diem tin nhan/de xuat DAU TIEN gui thanh cong (lazy-create) - KHONG phai luc mo man
     * soan tin, xem dac ta muc 2.
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
