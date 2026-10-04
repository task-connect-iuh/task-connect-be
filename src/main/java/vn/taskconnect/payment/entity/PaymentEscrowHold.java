package vn.taskconnect.payment.entity;

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
import vn.taskconnect.payment.api.EscrowHoldStatus;

/**
 * Mot ban ghi tam giu tien that cho 1 booking (payment_escrow_holds) - xem V48. posterId/taskerId
 * la SAO CHEP (denormalize) tu Booking luc tao (Payment khong goi dong bo nguoc ve Booking, xem
 * Javadoc PaymentFacade). status luon HELD o dot nay (xem Javadoc EscrowHoldStatus).
 */
@Entity
@Table(name = "payment_escrow_holds")
public class PaymentEscrowHold {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "booking_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID bookingId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "poster_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID posterId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "tasker_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID taskerId;

    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "held_amount", nullable = false)
    private long heldAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EscrowHoldStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentEscrowHold() {
        // JPA
    }

    /** Mo 1 ban ghi tam giu moi - luon bat dau HELD (gia lap luon thanh cong ngay, xem Javadoc EscrowHoldStatus). */
    public static PaymentEscrowHold open(UUID id, UUID bookingId, UUID posterId, UUID taskerId, long heldAmount,
            Instant now) {
        PaymentEscrowHold hold = new PaymentEscrowHold();
        hold.id = id;
        hold.bookingId = bookingId;
        hold.posterId = posterId;
        hold.taskerId = taskerId;
        hold.heldAmount = heldAmount;
        hold.status = EscrowHoldStatus.HELD;
        hold.createdAt = now;
        hold.updatedAt = now;
        return hold;
    }

    /** Tang so tien dang tam giu - dung khi Poster nap them sau khi 1 chi phi phat sinh duoc duyet (topUpToTarget). */
    public void increaseHeldAmount(long delta, Instant now) {
        this.heldAmount += delta;
        this.updatedAt = now;
    }

    /** Id noi bo cua ban ghi tam giu nay. */
    public UUID getId() {
        return id;
    }

    /** Id booking lien quan - UNIQUE, moi booking toi da 1 ban ghi. */
    public UUID getBookingId() {
        return bookingId;
    }

    /** Id tai khoan Poster - nguoi tien duoc giu tu vi. */
    public UUID getPosterId() {
        return posterId;
    }

    /** Id tai khoan Tasker - nguoi se nhan tien khi giai ngan (ngoai pham vi dot nay). */
    public UUID getTaskerId() {
        return taskerId;
    }

    /** Tong dang tam giu hien tai, don vi dong. */
    public long getHeldAmount() {
        return heldAmount;
    }

    /** Trang thai hien tai - luon HELD o dot nay. */
    public EscrowHoldStatus getStatus() {
        return status;
    }

    /** Thoi diem tao, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem cap nhat gan nhat. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
