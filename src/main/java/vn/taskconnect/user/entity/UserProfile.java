package vn.taskconnect.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.taskconnect.user.api.KycStatus;

/**
 * Ho so ca nhan cua mot tai khoan. Xem V2__create_user_tables.sql. Diem uy tin KHONG
 * luu o day (doc tu review.reputation_scores khi module Review ton tai), dung nhu comment
 * cua migration da ghi ro.
 */
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "address_text", length = 500)
    private String addressText;

    @Column(name = "operating_area", nullable = false)
    private String operatingArea;

    @Column(name = "location_lat", precision = 10, scale = 7)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 10, scale = 7)
    private BigDecimal locationLng;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {
        // JPA
    }

    /**
     * Tao khung ho so moi voi cac truong bat buoc toi thieu, luon bat dau o kycStatus
     * NOT_SUBMITTED nhu default cua cot trong V2. Goi updateDetails() ngay sau do de dien
     * cac truong con lai - khong lap lai fullName/operatingArea o day de tranh gan hai lan.
     */
    public UserProfile(UUID id, UUID accountId, String fullName, String operatingArea, Instant now) {
        this.id = id;
        this.accountId = accountId;
        this.fullName = fullName;
        this.operatingArea = operatingArea;
        this.kycStatus = KycStatus.NOT_SUBMITTED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Cap nhat cac truong ho so co the sua duoc tu API PATCH /users/me. locationLat/Lng
     * co the null neu nguoi dung khong khai bao toa do.
     */
    public void updateDetails(String fullName, String avatarUrl, String addressText, String operatingArea,
            BigDecimal locationLat, BigDecimal locationLng, Instant now) {
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.addressText = addressText;
        this.operatingArea = operatingArea;
        this.locationLat = locationLat;
        this.locationLng = locationLng;
        this.updatedAt = now;
    }

    /**
     * Dong bo trang thai KYC sau khi module KYC (Buoc 4) duyet hoac tu choi ho so.
     */
    public void changeKycStatus(KycStatus kycStatus, Instant now) {
        this.kycStatus = kycStatus;
        this.updatedAt = now;
    }

    /** Id noi bo cua ban ghi ho so, khac voi accountId (khoa ngoai sang auth_accounts). */
    public UUID getId() {
        return id;
    }

    /** Id tai khoan so huu ho so nay, unique trong user_profiles. */
    public UUID getAccountId() {
        return accountId;
    }

    /** Ho ten hien thi, bat buoc phai co gia tri (NOT NULL trong schema). */
    public String getFullName() {
        return fullName;
    }

    /** URL anh dai dien, null neu chua tung tai len. */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /** Dia chi day du dang van ban tu do, null neu chua khai bao. */
    public String getAddressText() {
        return addressText;
    }

    /** Khu vuc hoat dong, bat buoc phai co gia tri (NOT NULL trong schema). */
    public String getOperatingArea() {
        return operatingArea;
    }

    /** Vi do toa do hoat dong, null neu nguoi dung khong khai bao toa do. */
    public BigDecimal getLocationLat() {
        return locationLat;
    }

    /** Kinh do toa do hoat dong, null neu nguoi dung khong khai bao toa do. */
    public BigDecimal getLocationLng() {
        return locationLng;
    }

    /** Trang thai KYC hien tai, dong bo tu module KYC (Buoc 4) khi duoc duyet/tu choi. */
    public KycStatus getKycStatus() {
        return kycStatus;
    }

    /** Thoi diem tao ho so lan dau, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem cap nhat gan nhat. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
