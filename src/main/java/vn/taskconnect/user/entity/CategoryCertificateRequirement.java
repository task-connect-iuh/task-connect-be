package vn.taskconnect.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Chung chi nao duoc chap nhan cho nhom dich vu nao (Master Data), seed san trong
 * V5__seed_user_certificate_types.sql. QUAN HE OR trong cung mot category_id: cac dong
 * isMandatory=true la cac lua chon THAY THE nhau, Tasker chi can duoc duyet DUNG MOT trong
 * so do (khong phai het tat ca) de ho so ky nang cua category do chuyen VERIFIED - xem
 * comment dau file migration va docs/PROGRESS-USER-MODULE.md Buoc 6.
 */
@Entity
@Table(name = "user_category_certificate_requirements")
public class CategoryCertificateRequirement {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "category_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID categoryId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "certificate_type_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID certificateTypeId;

    @Column(name = "is_mandatory", nullable = false)
    private boolean mandatory;

    /**
     * @JdbcTypeCode(SqlTypes.TINYINT) bat buoc: cot that la TINYINT UNSIGNED (V2 migration),
     * cung mau da dung o ServiceCategory.minExperienceYears (Buoc 2) - tranh lap lai loi
     * lech kieu cot chi phat hien duoc khi chay DB that.
     */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "min_experience_years", nullable = false)
    private int minExperienceYears;

    protected CategoryCertificateRequirement() {
        // JPA
    }

    /** Id noi bo cua dong yeu cau nay. */
    public UUID getId() {
        return id;
    }

    /** Id nhom dich vu (user_service_categories) ap dung yeu cau nay. */
    public UUID getCategoryId() {
        return categoryId;
    }

    /** Id loai chung chi (user_certificate_types) duoc chap nhan cho category nay. */
    public UUID getCertificateTypeId() {
        return certificateTypeId;
    }

    /** True neu day la mot trong cac lua chon bat buoc (quan he OR voi cac dong cung category_id). */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * Muc kinh nghiem toi thieu (nam) rieng cho cap category+chung chi nay - khac cot
     * min_experience_years chung o ServiceCategory. Hien tai la 0 cho moi dong, cho OQ-04.
     */
    public int getMinExperienceYears() {
        return minExperienceYears;
    }
}
