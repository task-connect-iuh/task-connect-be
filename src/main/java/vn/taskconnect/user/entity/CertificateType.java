package vn.taskconnect.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Loai chung chi hanh nghe (Master Data), seed san trong
 * V5__seed_user_certificate_types.sql. Khong co created_at/updated_at, khong sua qua API -
 * cung nguyen tac Master Data nhu ServiceCategory (Buoc 2).
 */
@Entity
@Table(name = "user_certificate_types")
public class CertificateType {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 50, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "issuing_authority", length = 255)
    private String issuingAuthority;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected CertificateType() {
        // JPA
    }

    /** Id noi bo cua loai chung chi, tham chieu boi user_category_certificate_requirements va user_tasker_certifications. */
    public UUID getId() {
        return id;
    }

    /** Ma nghiep vu ngan gon, duy nhat, on dinh. */
    public String getCode() {
        return code;
    }

    /** Ten day du cua loai chung chi/van bang. */
    public String getName() {
        return name;
    }

    /** Loai co quan/don vi cap, null neu khong ro. */
    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    /** Mo ta them, null neu khong co. */
    public String getDescription() {
        return description;
    }

    /** Con hien hanh hay khong - loai chung chi bi vo hieu hoa khong hien trong danh sach chon. */
    public boolean isActive() {
        return active;
    }
}
