package vn.taskconnect.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Ghi nhan CCCD (theo id_number_hash) nao dang duoc mot tai khoan "chiem" (dang VERIFYING
 * hoac da VERIFIED) - PRIMARY KEY tren id_number_hash la lop chan cuoi cung o muc DB, chong
 * 2 tai khoan khac nhau cung dung 1 CCCD nop gan nhu dong thoi (xem
 * KycVerificationService.submitKyc). Khac KycVerification (moi lan nop la 1 dong rieng, khong
 * UNIQUE tren account_id), bang nay chi giu duy nhat 1 dong "dang hieu luc" cho moi CCCD: bi
 * xoa khi ho so lien quan bi tu choi/huy (giai phong CCCD do de nop lai), giu lai khi VERIFIED.
 */
@Entity
@Table(name = "user_kyc_id_number_locks")
public class KycIdNumberLock {

    @Id
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "id_number_hash", columnDefinition = "BINARY(32)", nullable = false, updatable = false)
    private byte[] idNumberHash;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID accountId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "kyc_verification_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID kycVerificationId;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    protected KycIdNumberLock() {
        // JPA
    }

    public KycIdNumberLock(byte[] idNumberHash, UUID accountId, UUID kycVerificationId, Instant claimedAt) {
        this.idNumberHash = idNumberHash;
        this.accountId = accountId;
        this.kycVerificationId = kycVerificationId;
        this.claimedAt = claimedAt;
    }

    /** Chuyen quyen "chiem" CCCD nay sang mot lan nop moi cua CHINH tai khoan da giu no truoc do. */
    public void reclaim(UUID kycVerificationId, Instant claimedAt) {
        this.kycVerificationId = kycVerificationId;
        this.claimedAt = claimedAt;
    }

    public byte[] getIdNumberHash() {
        return idNumberHash;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getKycVerificationId() {
        return kycVerificationId;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }
}
