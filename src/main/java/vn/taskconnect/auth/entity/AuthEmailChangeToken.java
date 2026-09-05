package vn.taskconnect.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Trang thai cua mot lan doi email dang dien ra, xem V14__create_email_change_tokens.sql.
 * Luong 2 buoc: (1) xac minh quyen so huu email HIEN TAI qua oldOtp, (2) sau khi xac minh
 * xong moi duoc nhap email moi va xac minh quyen so huu email do qua newOtp - chi khi ca hai
 * buoc thanh cong AuthService.confirmEmailChange() moi thuc su doi auth_accounts.email. Chi
 * giu 1 dong "dang hieu luc" cho moi tai khoan (khac AuthEmailVerificationToken append-only) -
 * AuthService xoa dong cu truoc khi tao dong moi moi lan bam "Doi email" tu dau.
 */
@Entity
@Table(name = "auth_email_change_tokens")
public class AuthEmailChangeToken {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "old_otp_hash", nullable = false)
    private String oldOtpHash;

    @Column(name = "old_otp_expires_at", nullable = false)
    private Instant oldOtpExpiresAt;

    @Column(name = "old_verified_at")
    private Instant oldVerifiedAt;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "old_attempt_count", nullable = false)
    private int oldAttemptCount;

    @Column(name = "new_email")
    private String newEmail;

    @Column(name = "new_otp_hash")
    private String newOtpHash;

    @Column(name = "new_otp_expires_at")
    private Instant newOtpExpiresAt;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "new_attempt_count", nullable = false)
    private int newAttemptCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuthEmailChangeToken() {
        // JPA
    }

    /** Khoi tao buoc 1: gui OTP toi email HIEN TAI de xac minh quyen so huu truoc. */
    public AuthEmailChangeToken(UUID id, UUID accountId, String oldOtpHash, Instant oldOtpExpiresAt,
            Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.oldOtpHash = oldOtpHash;
        this.oldOtpExpiresAt = oldOtpExpiresAt;
        this.oldAttemptCount = 0;
        this.newAttemptCount = 0;
        this.createdAt = createdAt;
    }

    public boolean isOldOtpExpired(Instant now) {
        return oldOtpExpiresAt.isBefore(now);
    }

    public boolean isOldVerified() {
        return oldVerifiedAt != null;
    }

    /** Nhap sai OTP email hien tai. Tra ve true neu vua cham nguong (buoc phai yeu cau lai tu dau). */
    public boolean registerOldFailedAttempt(int maxAttempts) {
        oldAttemptCount++;
        return oldAttemptCount >= maxAttempts;
    }

    public void markOldVerified(Instant now) {
        this.oldVerifiedAt = now;
    }

    /**
     * Buoc 2: da xac minh email cu xong, ghi nhan email moi ung vien va sinh OTP rieng gui
     * toi dia chi do. Reset newAttemptCount ve 0 - day co the la lan thu 2+ nhap email moi
     * (vd lan truoc go sai/doi y), khong duoc cong don so lan sai cua email moi khac.
     */
    public void challengeNewEmail(String newEmail, String newOtpHash, Instant newOtpExpiresAt) {
        this.newEmail = newEmail;
        this.newOtpHash = newOtpHash;
        this.newOtpExpiresAt = newOtpExpiresAt;
        this.newAttemptCount = 0;
    }

    public boolean isNewOtpExpired(Instant now) {
        return newOtpExpiresAt == null || newOtpExpiresAt.isBefore(now);
    }

    /** Nhap sai OTP email moi. Tra ve true neu vua cham nguong. */
    public boolean registerNewFailedAttempt(int maxAttempts) {
        newAttemptCount++;
        return newAttemptCount >= maxAttempts;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getOldOtpHash() {
        return oldOtpHash;
    }

    public String getNewEmail() {
        return newEmail;
    }

    public String getNewOtpHash() {
        return newOtpHash;
    }

    public Instant getNewOtpExpiresAt() {
        return newOtpExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
