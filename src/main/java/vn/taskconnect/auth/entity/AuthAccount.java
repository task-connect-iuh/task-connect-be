package vn.taskconnect.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.taskconnect.auth.api.AccountStatus;

/**
 * Bang goc dinh danh cua he thong. Xem V1__create_auth_tables.sql.
 */
@Entity
@Table(name = "auth_accounts")
public class AuthAccount {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AuthAccount() {
        // JPA
    }

    public AuthAccount(UUID id, String email, String phone, String passwordHash, AccountStatus status,
            Instant now) {
        this.id = id;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.status = status;
        this.failedLoginCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean isLocked(Instant now) {
        return status == AccountStatus.LOCKED && lockedUntil != null && lockedUntil.isAfter(now);
    }

    /**
     * Dang nhap sai. Sau {@code maxAttempts} lan lien tiep thi khoa tai khoan trong
     * {@code lockDuration}. Nguong nay dang hardcode tam trong AuthService, xem TODO tai do.
     */
    public void registerFailedLogin(Instant now, int maxAttempts, Duration lockDuration) {
        if (status == AccountStatus.LOCKED && lockedUntil != null && !lockedUntil.isAfter(now)) {
            status = AccountStatus.ACTIVE;
            failedLoginCount = 0;
        }
        failedLoginCount++;
        if (failedLoginCount >= maxAttempts) {
            status = AccountStatus.LOCKED;
            lockedUntil = now.plus(lockDuration);
        }
        updatedAt = now;
    }

    public void recordSuccessfulLogin(Instant now) {
        failedLoginCount = 0;
        lockedUntil = null;
        if (status == AccountStatus.LOCKED) {
            status = AccountStatus.ACTIVE;
        }
        lastLoginAt = now;
        updatedAt = now;
    }

    public void activate(Instant now) {
        if (status == AccountStatus.UNVERIFIED) {
            status = AccountStatus.ACTIVE;
            updatedAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
