package vn.taskconnect.booking.entity;

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
import vn.taskconnect.booking.api.BookingStatus;
import vn.taskconnect.booking.api.PaymentMethod;

/**
 * Mot booking-lite tao ra tu UC11 (Poster chon 1 Tasker). Xem
 * V31__create_booking_bookings_table.sql - CHI la khung toi thieu (khong dieu phoi lich, huy,
 * khieu nai), du de gan chat_channel va tinh fee_base. Tu 2026-10-02 (xem V49) co them
 * paymentMethod va status CONFIRMED (thay PENDING_ESCROW) - xem Javadoc BookingStatus/
 * PaymentMethod. Khong co method chuyen trang thai nao khac dot nay ngoai updateScheduledAt()
 * (doi lich, Round B6).
 */
@Entity
@Table(name = "booking_bookings")
public class Booking {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "application_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID applicationId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "task_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID taskId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "poster_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID posterId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "tasker_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID taskerId;

    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "fee_base_amount", nullable = false, updatable = false)
    private long feeBaseAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20, updatable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Booking() {
        // JPA
    }

    /**
     * Tao 1 booking-lite ngay sau khi Poster chon xong 1 ung vien o UC11 VA sau khi
     * PaymentFacade.holdInitial() da giu tien thanh cong (xem BookingFacadeImpl.
     * createFromApplication) - vi vay luon bat dau CONFIRMED tu 2026-10-02 (GUARDRAIL 2
     * CLAUDE.md da thoa man that su, khac PENDING_ESCROW truoc day). scheduledAt ke thua tu
     * task luc tao (co the doi sau qua RESCHEDULE_PROPOSAL, Round B6).
     */
    public static Booking createFromApplication(UUID id, UUID applicationId, UUID taskId, UUID posterId,
            UUID taskerId, long feeBaseAmount, PaymentMethod paymentMethod, Instant initialScheduledAt,
            Instant now) {
        Booking booking = new Booking();
        booking.id = id;
        booking.applicationId = applicationId;
        booking.taskId = taskId;
        booking.posterId = posterId;
        booking.taskerId = taskerId;
        booking.feeBaseAmount = feeBaseAmount;
        booking.paymentMethod = paymentMethod;
        booking.status = BookingStatus.CONFIRMED;
        booking.scheduledAt = initialScheduledAt;
        booking.createdAt = now;
        booking.updatedAt = now;
        return booking;
    }

    /** Cap nhat lich lam viec sau khi 1 RESCHEDULE_PROPOSAL duoc Dong y (Round B6) - khong dung den escrow/phi. */
    public void updateScheduledAt(Instant scheduledAt, Instant now) {
        this.scheduledAt = scheduledAt;
        this.updatedAt = now;
    }

    /** Id noi bo cua booking nay. */
    public UUID getId() {
        return id;
    }

    /** Id application da duoc chon o UC11 - 1-1 voi booking (UNIQUE trong V31). */
    public UUID getApplicationId() {
        return applicationId;
    }

    /** Id cong viec goc. */
    public UUID getTaskId() {
        return taskId;
    }

    /** Id tai khoan Task Poster. */
    public UUID getPosterId() {
        return posterId;
    }

    /** Id tai khoan Tasker duoc chon. */
    public UUID getTaskerId() {
        return taskerId;
    }

    /** So tien lam co so tinh phi nen tang (don vi dong) - xem dac ta muc 4. */
    public long getFeeBaseAmount() {
        return feeBaseAmount;
    }

    /** Phuong thuc thanh toan Poster da chon luc UC11 - xem Javadoc PaymentMethod. */
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    /** Trang thai hien tai - CONFIRMED tu 2026-10-02 (xem Javadoc BookingStatus). */
    public BookingStatus getStatus() {
        return status;
    }

    /** Thoi gian du kien thuc hien, co the doi qua RESCHEDULE_PROPOSAL. */
    public Instant getScheduledAt() {
        return scheduledAt;
    }

    /** Thoi diem tao booking, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem cap nhat gan nhat. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
