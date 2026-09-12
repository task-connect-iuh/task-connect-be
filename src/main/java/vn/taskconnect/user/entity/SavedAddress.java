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
import vn.taskconnect.user.api.LocationType;

/**
 * Mot dia chi Poster tu luu de chon nhanh khi dang viec (vd "Nha", "Cong ty"), giong so dia
 * chi giao hang cua Shopee. Xem V25__create_user_saved_addresses.sql - hoan toan doc lap voi
 * user_profiles.address_text va task_tasks.address_text, chi la danh sach goi y de dien
 * nhanh, khong dong bo nguoc lai.
 */
@Entity
@Table(name = "user_saved_addresses")
public class SavedAddress {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "address_text", nullable = false, length = 500)
    private String addressText;

    @Column(name = "lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 20)
    private LocationType locationType;

    @Column(name = "arrival_notes", length = 500)
    private String arrivalNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SavedAddress() {
        // JPA
    }

    /** Luu mot dia chi moi. */
    public SavedAddress(UUID id, UUID accountId, String label, String addressText, BigDecimal lat, BigDecimal lng,
            LocationType locationType, String arrivalNotes, Instant now) {
        this.id = id;
        this.accountId = accountId;
        this.label = label;
        this.addressText = addressText;
        this.lat = lat;
        this.lng = lng;
        this.locationType = locationType;
        this.arrivalNotes = arrivalNotes;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Sua lai mot dia chi da luu - PATCH mot phan da duoc gop san o SavedAddressService. */
    public void update(String label, String addressText, BigDecimal lat, BigDecimal lng, LocationType locationType,
            String arrivalNotes, Instant now) {
        this.label = label;
        this.addressText = addressText;
        this.lat = lat;
        this.lng = lng;
        this.locationType = locationType;
        this.arrivalNotes = arrivalNotes;
        this.updatedAt = now;
    }

    /** Id noi bo cua dia chi da luu. */
    public UUID getId() {
        return id;
    }

    /** Id tai khoan so huu dia chi nay. */
    public UUID getAccountId() {
        return accountId;
    }

    /** Ten goi nho nguoi dung tu dat, vd "Nha", "Cong ty". */
    public String getLabel() {
        return label;
    }

    /** Dia chi day du dang van ban. */
    public String getAddressText() {
        return addressText;
    }

    /** Vi do cua dia chi nay. */
    public BigDecimal getLat() {
        return lat;
    }

    /** Kinh do cua dia chi nay. */
    public BigDecimal getLng() {
        return lng;
    }

    /** Loai dia diem di kem, null neu khong khai bao. */
    public LocationType getLocationType() {
        return locationType;
    }

    /** Luu y khi toi dia chi nay, null neu khong khai bao. */
    public String getArrivalNotes() {
        return arrivalNotes;
    }

    /** Thoi diem luu dia chi nay lan dau. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem cap nhat gan nhat. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
