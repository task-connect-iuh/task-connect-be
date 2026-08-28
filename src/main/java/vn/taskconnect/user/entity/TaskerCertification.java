package vn.taskconnect.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.taskconnect.user.api.CertificationStatus;

/**
 * Mot lan nop chung chi hanh nghe cho MOT nhom dich vu (Buoc 6). Xem V2__create_user_tables.sql
 * - khong co UNIQUE tren (account_id, category_id), moi lan nop la mot dong rieng (giu lai
 * lich su cac lan bi tu choi truoc do), khac TaskerSkillProfile chi co dung mot dong moi
 * category. certificate_number_enc/file_url_enc da ma hoa boi
 * {@link vn.taskconnect.common.crypto.AesEncryptionService} (Buoc 3) - entity nay khong tu
 * ma hoa/giai ma.
 */
@Entity
@Table(name = "user_tasker_certifications")
public class TaskerCertification {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID accountId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "category_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID categoryId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "certificate_type_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID certificateTypeId;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "certificate_number_enc", columnDefinition = "VARBINARY(512)", updatable = false)
    private byte[] certificateNumberEnc;

    @Column(name = "issuing_authority", length = 255, updatable = false)
    private String issuingAuthority;

    @Column(name = "issued_date", updatable = false)
    private LocalDate issuedDate;

    @Column(name = "expiry_date", updatable = false)
    private LocalDate expiryDate;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "file_url_enc", columnDefinition = "VARBINARY(1024)", nullable = false, updatable = false)
    private byte[] fileUrlEnc;

    @Column(name = "experience_proof_url", length = 500, updatable = false)
    private String experienceProofUrl;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "claimed_experience_years", updatable = false)
    private Integer claimedExperienceYears;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CertificationStatus status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "reviewed_by_admin_id", columnDefinition = "BINARY(16)")
    private UUID reviewedByAdminId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected TaskerCertification() {
        // JPA
    }

    /** Tao mot lan nop chung chi moi, luon bat dau o PENDING_REVIEW (default cua cot trong V2). */
    public TaskerCertification(UUID id, UUID accountId, UUID categoryId, UUID certificateTypeId,
            byte[] certificateNumberEnc, String issuingAuthority, LocalDate issuedDate, LocalDate expiryDate,
            byte[] fileUrlEnc, String experienceProofUrl, Integer claimedExperienceYears, Instant submittedAt) {
        this.id = id;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.certificateTypeId = certificateTypeId;
        this.certificateNumberEnc = certificateNumberEnc;
        this.issuingAuthority = issuingAuthority;
        this.issuedDate = issuedDate;
        this.expiryDate = expiryDate;
        this.fileUrlEnc = fileUrlEnc;
        this.experienceProofUrl = experienceProofUrl;
        this.claimedExperienceYears = claimedExperienceYears;
        this.status = CertificationStatus.PENDING_REVIEW;
        this.submittedAt = submittedAt;
    }

    /** Admin duyet - chuyen APPROVED, ghi nhan ai duyet va luc nao. */
    public void approve(UUID adminId, Instant now) {
        this.status = CertificationStatus.APPROVED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = now;
    }

    /** Admin tu choi, bat buoc kem ly do - chuyen REJECTED. */
    public void reject(UUID adminId, String rejectionReason, Instant now) {
        this.status = CertificationStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = now;
    }

    /** Id noi bo cua lan nop nay, dung de Admin duyet/tu choi dung ban ghi. */
    public UUID getId() {
        return id;
    }

    /** Id tai khoan da nop chung chi nay. */
    public UUID getAccountId() {
        return accountId;
    }

    /** Id nhom dich vu ma chung chi nay dung de xac minh. */
    public UUID getCategoryId() {
        return categoryId;
    }

    /** Id loai chung chi (user_certificate_types) da chon khi nop. */
    public UUID getCertificateTypeId() {
        return certificateTypeId;
    }

    /** So hieu chung chi da ma hoa, null neu khong khai bao. */
    public byte[] getCertificateNumberEnc() {
        return certificateNumberEnc;
    }

    /** Co quan cap chung chi Tasker tu khai, null neu khong khai bao. */
    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    /** Ngay cap, null neu khong khai bao. */
    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    /** Ngay het han, null = vo thoi han. */
    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    /** Object key S3 (da ma hoa) cua file anh/PDF chung chi, khong phai URL cong khai. */
    public byte[] getFileUrlEnc() {
        return fileUrlEnc;
    }

    /** URL minh chung kinh nghiem bo sung (khong ma hoa - khong phai du lieu dinh danh), null neu khong co. */
    public String getExperienceProofUrl() {
        return experienceProofUrl;
    }

    /** So nam kinh nghiem Tasker tu khai kem chung chi nay, null neu khong khai bao. */
    public Integer getClaimedExperienceYears() {
        return claimedExperienceYears;
    }

    /** Trang thai xet duyet hien tai cua lan nop nay. */
    public CertificationStatus getStatus() {
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

    /** Thoi diem nop lan nay. */
    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
