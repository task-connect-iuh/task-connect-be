package vn.taskconnect.user.entity;

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
import vn.taskconnect.user.api.KycStatus;

/**
 * Mot lan nop xac minh danh tinh (CCCD) cua mot tai khoan. Xem V2__create_user_tables.sql -
 * khong co UNIQUE tren account_id, moi lan nop la mot dong rieng (cho phep nop lai sau khi
 * bi tu choi), ban ghi moi nhat theo submitted_at la trang thai hien hanh. Cac truong _enc
 * la du lieu da ma hoa AES-256-GCM boi {@link vn.taskconnect.common.crypto.AesEncryptionService}
 * (Buoc 3) - entity nay khong tu ma hoa/giai ma, chi luu/doc blob nhi phan.
 */
@Entity
@Table(name = "user_kyc_verifications")
public class KycVerification {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "full_name_on_id", nullable = false, length = 150, updatable = false)
    private String fullNameOnId;

    // @JdbcTypeCode(SqlTypes.VARBINARY) bat buoc: byte[] khong co chu thich se khien
    // Hibernate suy doan kieu JDBC khac VARBINARY that trong V2 migration, dan den
    // ddl-auto=validate chan khoi dong ung dung (cung loai loi da gap voi TINYINT UNSIGNED
    // o ServiceCategory.minExperienceYears, xem docs/PROGRESS-USER-MODULE.md).
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "id_number_enc", columnDefinition = "VARBINARY(512)", nullable = false, updatable = false)
    private byte[] idNumberEnc;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "id_card_front_url_enc", columnDefinition = "VARBINARY(1024)", nullable = false, updatable = false)
    private byte[] idCardFrontUrlEnc;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "id_card_back_url_enc", columnDefinition = "VARBINARY(1024)", nullable = false, updatable = false)
    private byte[] idCardBackUrlEnc;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private KycStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "reviewed_by_admin_id", columnDefinition = "BINARY(16)")
    private UUID reviewedByAdminId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected KycVerification() {
        // JPA
    }

    /** Tao mot lan nop moi, luon bat dau o trang thai VERIFYING (default cua cot trong V2). */
    public KycVerification(UUID id, UUID accountId, String fullNameOnId, byte[] idNumberEnc,
            byte[] idCardFrontUrlEnc, byte[] idCardBackUrlEnc, Instant submittedAt) {
        this.id = id;
        this.accountId = accountId;
        this.fullNameOnId = fullNameOnId;
        this.idNumberEnc = idNumberEnc;
        this.idCardFrontUrlEnc = idCardFrontUrlEnc;
        this.idCardBackUrlEnc = idCardBackUrlEnc;
        this.status = KycStatus.VERIFYING;
        this.submittedAt = submittedAt;
    }

    /** Admin duyet ho so - chuyen trang thai sang VERIFIED, ghi nhan ai duyet va luc nao. */
    public void approve(UUID adminId, Instant now) {
        this.status = KycStatus.VERIFIED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = now;
    }

    /** Admin tu choi ho so, bat buoc kem ly do - chuyen trang thai sang REJECTED. */
    public void reject(UUID adminId, String rejectionReason, Instant now) {
        this.status = KycStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = now;
    }

    /** Id noi bo cua lan nop nay, dung de Admin duyet/tu choi dung ban ghi. */
    public UUID getId() {
        return id;
    }

    /** Id tai khoan da nop ho so nay. */
    public UUID getAccountId() {
        return accountId;
    }

    /** Ho ten in tren CCCD, co the khac fullName hien thi o user_profiles. */
    public String getFullNameOnId() {
        return fullNameOnId;
    }

    /** So CCCD da ma hoa (blob IV+ciphertext+tag), giai ma bang AesEncryptionService khi can hien thi. */
    public byte[] getIdNumberEnc() {
        return idNumberEnc;
    }

    /** Object key S3 (da ma hoa) cua anh mat truoc CCCD, khong phai URL cong khai. */
    public byte[] getIdCardFrontUrlEnc() {
        return idCardFrontUrlEnc;
    }

    /** Object key S3 (da ma hoa) cua anh mat sau CCCD, khong phai URL cong khai. */
    public byte[] getIdCardBackUrlEnc() {
        return idCardBackUrlEnc;
    }

    /** Trang thai xet duyet hien tai cua lan nop nay (VERIFYING/VERIFIED/REJECTED). */
    public KycStatus getStatus() {
        return status;
    }

    /** Ly do tu choi, null neu chua bi tu choi. */
    public String getRejectionReason() {
        return rejectionReason;
    }

    /** Id tai khoan Admin da duyet/tu choi, null neu chua duoc xem xet. */
    public UUID getReviewedByAdminId() {
        return reviewedByAdminId;
    }

    /** Thoi diem duoc duyet/tu choi, null neu chua duoc xem xet. */
    public Instant getReviewedAt() {
        return reviewedAt;
    }

    /** Thoi diem nop ho so nay. */
    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
