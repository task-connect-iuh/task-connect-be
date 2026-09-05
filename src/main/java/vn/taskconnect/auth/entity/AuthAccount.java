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

    /**
     * Dinh danh Google (claim "sub" trong ID token) - NULL neu tai khoan chua tung dang nhap
     * qua Google. Duy nhat tren toan bang, xem uq_auth_accounts_google_id trong
     * V11__add_google_oauth_to_auth_accounts.sql.
     */
    @Column(name = "google_id")
    private String googleId;

    /**
     * Bcrypt hash. Co the NULL neu tai khoan duoc tao qua dang nhap Google va chua tung dat
     * mat khau (xem AuthService.loginWithGoogle()) - login() bang mat khau tren tai khoan nay
     * se luon that bai an toan vi BCryptPasswordEncoder.matches() tra ve false voi hash NULL.
     */
    @Column(name = "password_hash", length = 72)
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

    /**
     * Tao tai khoan moi qua dang nhap Google lan dau (AuthService.loginWithGoogle()) - khong
     * co mat khau (passwordHash NULL) va vao thang trang thai ACTIVE, bo qua UNVERIFIED/OTP:
     * Google da tu xac thuc quyen so huu email nay truoc khi phat ID token, buoc xac minh
     * email rieng cua he thong la thua.
     */
    public static AuthAccount createFromGoogle(UUID id, String email, String googleId, Instant now) {
        AuthAccount account = new AuthAccount(id, email, null, null, AccountStatus.ACTIVE, now);
        account.googleId = googleId;
        return account;
    }

    /**
     * Gan google_id vao mot tai khoan mat khau da co san (email trung voi tai khoan Google
     * dang dang nhap) - chi goi sau khi nguoi dung da xac nhan qua man hoi lai kieu GitHub o
     * FE (AuthService.confirmGoogleLink()), khong bao gio goi ngam dinh. Tai khoan dang
     * UNVERIFIED duoc chuyen thang ACTIVE, cung ly do voi resetPassword(): email Google la
     * bang chung xac thuc manh hon OTP thong thuong.
     */
    public void linkGoogleId(String googleId, Instant now) {
        this.googleId = googleId;
        if (status == AccountStatus.UNVERIFIED) {
            status = AccountStatus.ACTIVE;
        }
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

    /**
     * Dat mat khau moi sau khi xac minh OTP quen mat khau thanh cong. Xoa luon khoa tam
     * thoi (failedLoginCount, lockedUntil, chuyen LOCKED ve ACTIVE) - nguoi dung da chung
     * minh quyen so huu tai khoan qua OTP gui toi email, bang chung manh hon ca mat khau
     * cu, giu khoa 15 phut sau do la vo ly. Cung chuyen UNVERIFIED ve ACTIVE vi cung ly do:
     * OTP gui toi email la bang chung xac minh email manh hon ca luong verify-email thong
     * thuong, khong co ly do bat nguoi dung xac minh lai email lan nua.
     */
    public void resetPassword(String newPasswordHash, Instant now) {
        this.passwordHash = newPasswordHash;
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        if (status == AccountStatus.LOCKED || status == AccountStatus.UNVERIFIED) {
            status = AccountStatus.ACTIVE;
        }
        this.updatedAt = now;
    }

    /**
     * Doi email sau khi da xac minh quyen so huu ca email cu lan email moi qua OTP (xem
     * AuthService.confirmEmailChange()) - UNIQUE KEY tren cot email (V1__create_auth_tables.sql)
     * la lop chan cuoi cung chong race condition, giong updatePhone().
     */
    public void changeEmail(String email, Instant now) {
        this.email = email;
        this.updatedAt = now;
    }

    /**
     * Doi so dien thoai sau khi da dang ky - AuthService.updatePhone() da kiem tra trung
     * truoc khi goi ham nay (existsByPhoneAndIdNot), UNIQUE KEY tren cot phone (xem
     * V1__create_auth_tables.sql) la lop chan cuoi cung chong race condition.
     */
    public void updatePhone(String phone, Instant now) {
        this.phone = phone;
        this.updatedAt = now;
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

    public String getGoogleId() {
        return googleId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
