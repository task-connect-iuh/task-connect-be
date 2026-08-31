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
 * Ma OTP dat lai mat khau, luu duoi dang BCrypt. Xem V4__create_password_reset_tokens.sql.
 *
 * <p>Mo hinh append-only giong het AuthEmailVerificationToken: gui lai ma khong cap nhat
 * tai cho ma cu, ma cu danh dau {@code usedAt} roi tao dong moi.
 */
@Entity
@Table(name = "auth_password_reset_tokens")
public class AuthPasswordResetToken {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "token_hash", nullable = false, updatable = false)
    private String tokenHash;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuthPasswordResetToken() {
        // JPA
    }

    public AuthPasswordResetToken(UUID id, UUID accountId, String tokenHash, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.tokenHash = tokenHash;
        this.attemptCount = 0;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    /**
     * Nhap sai mot lan. Cham nguong thi ma tu vo hieu hoa ngay (usedAt), nguoi dung phai
     * yeu cau ma moi - lop chan brute-force chinh cho OTP 6 chu so, xem AuthService.resetPassword.
     *
     * @return true neu ma vua bi vo hieu hoa boi lan nhap sai nay
     */
    public boolean registerFailedAttempt(Instant now, int maxAttempts) {
        attemptCount++;
        if (attemptCount >= maxAttempts) {
            markUsed(now);
            return true;
        }
        return false;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
