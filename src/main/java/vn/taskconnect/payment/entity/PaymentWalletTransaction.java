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

/**
 * Mot dong so giao dich vi (payment_wallet_transactions), CHI GHI THEM - khong UPDATE/DELETE
 * dong da co (quy tac 3 cua .claude/rules/14-payment-escrow.md). amount co dau: duong la tien vao
 * vi (DEPOSIT_SIMULATED), am la tien ra khoi vi de chuyen vao tam giu (ESCROW_HOLD/ESCROW_TOPUP).
 * Xem V47.
 */
@Entity
@Table(name = "payment_wallet_transactions")
public class PaymentWalletTransaction {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "wallet_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30, updatable = false)
    private WalletTransactionType type;

    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "amount", nullable = false, updatable = false)
    private long amount;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "reference_booking_id", columnDefinition = "BINARY(16)", updatable = false)
    private UUID referenceBookingId;

    @Column(name = "note", length = 255, updatable = false)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentWalletTransaction() {
        // JPA
    }

    private static PaymentWalletTransaction of(UUID id, UUID walletId, WalletTransactionType type, long amount,
            UUID referenceBookingId, String note, Instant now) {
        PaymentWalletTransaction transaction = new PaymentWalletTransaction();
        transaction.id = id;
        transaction.walletId = walletId;
        transaction.type = type;
        transaction.amount = amount;
        transaction.referenceBookingId = referenceBookingId;
        transaction.note = note;
        transaction.createdAt = now;
        return transaction;
    }

    /** Ghi nhan tien gia lap "nap vao" vi (tang balance) - buoc 1 cua holdInitial()/topUpToTarget() trong PaymentFacadeImpl. */
    public static PaymentWalletTransaction depositSimulated(UUID id, UUID walletId, long amount,
            UUID referenceBookingId, String note, Instant now) {
        return of(id, walletId, WalletTransactionType.DEPOSIT_SIMULATED, amount, referenceBookingId, note, now);
    }

    /** Ghi nhan tien chuyen tu vi sang tam giu LAN DAU (amount am) - buoc 2 cua holdInitial(). */
    public static PaymentWalletTransaction escrowHold(UUID id, UUID walletId, long amount, UUID referenceBookingId,
            Instant now) {
        return of(id, walletId, WalletTransactionType.ESCROW_HOLD, -amount, referenceBookingId, null, now);
    }

    /** Ghi nhan tien chuyen tu vi sang tam giu do NAP THEM (amount am) - buoc 2 cua topUpToTarget(). */
    public static PaymentWalletTransaction escrowTopUp(UUID id, UUID walletId, long amount, UUID referenceBookingId,
            Instant now) {
        return of(id, walletId, WalletTransactionType.ESCROW_TOPUP, -amount, referenceBookingId, null, now);
    }

    /** Id noi bo cua dong giao dich nay. */
    public UUID getId() {
        return id;
    }

    /** Id vi lien quan. */
    public UUID getWalletId() {
        return walletId;
    }

    /** So tien, co dau: duong la vao vi, am la ra khoi vi. */
    public long getAmount() {
        return amount;
    }

    /** Id booking lien quan, null neu giao dich khong gan booking nao. */
    public UUID getReferenceBookingId() {
        return referenceBookingId;
    }

    /** Ghi chu kem theo, null neu khong co. */
    public String getNote() {
        return note;
    }

    /** Thoi diem ghi nhan, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
