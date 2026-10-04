package vn.taskconnect.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Vi gia lap cua 1 tai khoan (payment_wallets) - xem V47. balance la so du KHA DUNG (chua tru
 * phan dang tam giu) - KHONG duoc UPDATE truc tiep ngoai 2 method deposit()/withdraw() o day, va
 * moi lan goi phai kem dung 1 dong payment_wallet_transactions trong CUNG 1 transaction (quy tac
 * 2/3 cua .claude/rules/14-payment-escrow.md), xem PaymentFacadeImpl.
 */
@Entity
@Table(name = "payment_wallets")
public class PaymentWallet {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID accountId;

    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "balance", nullable = false)
    private long balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentWallet() {
        // JPA
    }

    /** Tao vi moi, so du 0 - dung luc lazy-create (lan dau mot tai khoan can toi vi, xem PaymentFacadeImpl.findOrCreateWallet). */
    public static PaymentWallet createEmpty(UUID id, UUID accountId, Instant now) {
        PaymentWallet wallet = new PaymentWallet();
        wallet.id = id;
        wallet.accountId = accountId;
        wallet.balance = 0;
        wallet.createdAt = now;
        wallet.updatedAt = now;
        return wallet;
    }

    /** Tang so du - LUON phai di kem 1 dong payment_wallet_transactions ghi lai, khong tu goi rieng le. */
    public void deposit(long amount, Instant now) {
        this.balance += amount;
        this.updatedAt = now;
    }

    /** Giam so du (chuyen sang tam giu) - LUON phai di kem 1 dong payment_wallet_transactions ghi lai. */
    public void withdraw(long amount, Instant now) {
        this.balance -= amount;
        this.updatedAt = now;
    }

    /** Id noi bo cua vi nay. */
    public UUID getId() {
        return id;
    }

    /** Id tai khoan so huu vi nay - UNIQUE, moi tai khoan toi da 1 vi. */
    public UUID getAccountId() {
        return accountId;
    }

    /** So du kha dung hien tai, don vi dong. */
    public long getBalance() {
        return balance;
    }

    /** Thoi diem tao vi, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem cap nhat gan nhat. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
