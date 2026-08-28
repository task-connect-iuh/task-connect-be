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
import vn.taskconnect.user.api.SkillVerificationStatus;

/**
 * Ho so ky nang cua mot Tasker cho MOT nhom dich vu (Buoc 6). Xem V2__create_user_tables.sql
 * - UNIQUE (account_id, category_id), chi co dung mot dong cho moi cap Tasker+category, khac
 * KycVerification (Buoc 4) khong co UNIQUE tren account_id. Vi vay nop lai sau khi bi tu
 * choi la UPDATE lai chinh dong nay (xem resubmit()), khong tao dong moi.
 */
@Entity
@Table(name = "user_tasker_skill_profiles")
public class TaskerSkillProfile {

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

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "years_experience", nullable = false)
    private int yearsExperience;

    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "price_min")
    private Long priceMin;

    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "price_max")
    private Long priceMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private SkillVerificationStatus verificationStatus;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskerSkillProfile() {
        // JPA
    }

    /** Tao ho so ky nang moi cho mot category, luon bat dau o PENDING (default cua cot trong V2). */
    public TaskerSkillProfile(UUID id, UUID accountId, UUID categoryId, int yearsExperience,
            Long priceMin, Long priceMax, Instant now) {
        this.id = id;
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.yearsExperience = yearsExperience;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.verificationStatus = SkillVerificationStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Nop lai sau khi bi REJECTED - cap nhat lai thong tin kinh nghiem/gia va dua trang
     * thai ve PENDING de cho xet duyet lan chung chi moi. Chi goi duoc khi dang REJECTED,
     * kiem tra dieu kien nay o TaskerSkillService, khong phai o entity.
     */
    public void resubmit(int yearsExperience, Long priceMin, Long priceMax, Instant now) {
        this.yearsExperience = yearsExperience;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.verificationStatus = SkillVerificationStatus.PENDING;
        this.updatedAt = now;
    }

    /** Chuyen VERIFIED khi mot chung chi hop le cua category nay duoc Admin duyet (quan he OR). */
    public void markVerified(Instant now) {
        this.verificationStatus = SkillVerificationStatus.VERIFIED;
        this.verifiedAt = now;
        this.updatedAt = now;
    }

    /** Chuyen REJECTED khi chung chi dang cho duyet cua category nay bi Admin tu choi. */
    public void markRejected(Instant now) {
        this.verificationStatus = SkillVerificationStatus.REJECTED;
        this.updatedAt = now;
    }

    /** Id noi bo cua ho so ky nang nay. */
    public UUID getId() {
        return id;
    }

    /** Id tai khoan Tasker so huu ho so nay. */
    public UUID getAccountId() {
        return accountId;
    }

    /** Id nhom dich vu ho so nay khai bao. */
    public UUID getCategoryId() {
        return categoryId;
    }

    /** So nam kinh nghiem Tasker tu khai. */
    public int getYearsExperience() {
        return yearsExperience;
    }

    /** Gia toi thieu Tasker chao (don vi dong), null neu khong khai bao. */
    public Long getPriceMin() {
        return priceMin;
    }

    /** Gia toi da Tasker chao (don vi dong), null neu khong khai bao. */
    public Long getPriceMax() {
        return priceMax;
    }

    /** Trang thai xac minh hien tai cua ho so ky nang nay. */
    public SkillVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    /** Thoi diem duoc xac minh VERIFIED, null neu chua tung duoc xac minh. */
    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    /** Thoi diem tao ho so lan dau. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem cap nhat gan nhat. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
