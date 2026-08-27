package vn.taskconnect.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Danh muc nhom dich vu (Master Data), seed san 5 nhom dien-nuoc trong
 * V4__seed_user_service_categories.sql. description/keywords la kho tri thuc RAG cho module
 * AI phan loai cong viec (xem 12-database.md muc "Mot nguon su that"). Khong co
 * created_at/updated_at - bang Master Data hiem khi doi, sua bang migration moi, khong sua
 * qua API.
 */
@Entity
@Table(name = "user_service_categories")
public class ServiceCategory {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 50, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    /**
     * @JdbcTypeCode(SqlTypes.TINYINT) bat buoc: cot that la TINYINT UNSIGNED (V2 migration),
     * int Java mac dinh anh xa sang INTEGER, Hibernate ddl-auto=validate se sap luc khoi
     * dong vi lech kieu cot (phat hien luc chay app that lan dau) - cung mau da dung o
     * AuthAccount.failedLoginCount.
     */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "min_experience_years", nullable = false)
    private int minExperienceYears;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected ServiceCategory() {
        // JPA
    }

    /** Id noi bo cua danh muc, tham chieu boi cac bang khac qua category_id. */
    public UUID getId() {
        return id;
    }

    /** Ma nghiep vu ngan gon, duy nhat, on dinh (dung de tra cuu thay vi hardcode UUID). */
    public String getCode() {
        return code;
    }

    /** Ten hien thi tieng Viet cua nhom dich vu. */
    public String getName() {
        return name;
    }

    /** Mo ta chi tiet, mot phan kho tri thuc RAG cho module AI phan loai cong viec. */
    public String getDescription() {
        return description;
    }

    /** Danh sach tu khoa lien quan, phan con lai cua kho tri thuc RAG. */
    public String getKeywords() {
        return keywords;
    }

    /**
     * Muc kinh nghiem toi thieu (nam) de Tasker duoc xac minh cho nhom nay - nguon su that
     * duy nhat cho gia tri nay (12-database.md). Hien tai la 0 cho ca 5 nhom, cho quyet dinh
     * OQ-04 (docs/OPEN-QUESTIONS.md) chot muc rieng cho Dien lanh va Dien cong nghiep.
     */
    public int getMinExperienceYears() {
        return minExperienceYears;
    }

    /** Con hien hanh hay khong - danh muc bi vo hieu hoa khong hien trong danh sach chon. */
    public boolean isActive() {
        return active;
    }
}
