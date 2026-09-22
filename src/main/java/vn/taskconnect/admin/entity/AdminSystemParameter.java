package vn.taskconnect.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Mot nguong van hanh dang key/value (vd platform_fee_rate, invite_expiry_hours). Xem
 * V28__create_admin_system_parameters.sql va .claude/rules/02-source-of-truth.md - moi
 * nguong nghiep vu phai doc qua bang nay, khong hardcode trong Java. Chi doc trong dot nay
 * (chua co man Admin chinh tham so), nen entity chua co method thay doi gia tri.
 */
@Entity
@Table(name = "admin_system_parameters")
public class AdminSystemParameter {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "param_key", nullable = false, updatable = false, length = 100)
    private String paramKey;

    @Column(name = "param_value", nullable = false, length = 100)
    private String paramValue;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AdminSystemParameter() {
        // JPA
    }

    /** Ten khoa tham so (vd "platform_fee_rate"), duy nhat theo UNIQUE constraint. */
    public String getParamKey() {
        return paramKey;
    }

    /** Gia tri hien tai, luon luu dang chuoi - noi doc (AdminFacadeImpl) tu parse dung kieu can dung. */
    public String getParamValue() {
        return paramValue;
    }

    /** Mo ta y nghia tham so, dung cho man Admin sau nay - co the null. */
    public String getDescription() {
        return description;
    }
}
