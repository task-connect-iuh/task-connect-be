package vn.taskconnect.task.entity;

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
import vn.taskconnect.task.api.SuppliesStatus;
import vn.taskconnect.task.api.TaskAiFlagReason;
import vn.taskconnect.task.api.TaskStatus;
import vn.taskconnect.user.api.LocationType;

/**
 * Mot cong viec do Task Poster dang. Xem V18__create_task_tables.sql. Dot 1 (dang viec toi
 * gian) chi ho tro tao va doc - khong co method chuyen trang thai nao khac ngoai createOpen()
 * (sua/huy UC07, giao viec UC11 se them method rieng, theo dung mau
 * KycVerification.approve()/reject() khi lam dot do). addressText/lat/lng/locationType/
 * arrivalNotes la thong tin noi CAN THUC HIEN cong viec, hoan toan doc lap voi cac truong
 * cung ten cua user_profiles - FE dien san tu ho so luc mo form dang viec (xem V24) nhung
 * khong tham chieu qua lai sau do. LocationType tai su dung enum cua module User qua goi
 * api/ (xem .claude/rules/00-architecture.md), khong dinh nghia enum rieng vi cung mot khai
 * niem nghiep vu. suppliesStatus bat buoc, suppliesNote luon tuy chon du suppliesStatus la gia
 * tri nao (xem V26__add_supplies_fields_to_task_tasks.sql).
 */
@Entity
@Table(name = "task_tasks")
public class Task {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "poster_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID posterId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "category_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID categoryId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "supplies_status", nullable = false, length = 20)
    private SuppliesStatus suppliesStatus;

    @Column(name = "supplies_note", columnDefinition = "TEXT")
    private String suppliesNote;

    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "budget_amount")
    private Long budgetAmount;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    /**
     * Chi de hien thi cho Tasker, KHONG co nghia he thong - he thong dang chi ho tro dung 1
     * Tasker/cong viec (rang buoc escrow/booking hien tai), xem OQ-08 (docs/OPEN-QUESTIONS.md)
     * da chot tam trong task-connect-claude/docs/TASK-MODULE-SPLIT.md.
     */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "estimated_workers_needed", nullable = false)
    private int estimatedWorkersNeeded;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "needs_admin_review", nullable = false)
    private boolean needsAdminReview;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_flag_reason", length = 30)
    private TaskAiFlagReason aiFlagReason;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "reviewed_by_admin_id", columnDefinition = "BINARY(16)")
    private UUID reviewedByAdminId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {
        // JPA
    }

    private Task(UUID id, UUID posterId, UUID categoryId, String title, String description, String addressText,
            BigDecimal lat, BigDecimal lng, LocationType locationType, String arrivalNotes,
            SuppliesStatus suppliesStatus, String suppliesNote, Long budgetAmount, Instant scheduledAt,
            int estimatedWorkersNeeded, TaskStatus status, Instant now) {
        this.id = id;
        this.posterId = posterId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.addressText = addressText;
        this.lat = lat;
        this.lng = lng;
        this.locationType = locationType;
        this.arrivalNotes = arrivalNotes;
        this.suppliesStatus = suppliesStatus;
        this.suppliesNote = suppliesNote;
        this.budgetAmount = budgetAmount;
        this.scheduledAt = scheduledAt;
        this.estimatedWorkersNeeded = estimatedWorkersNeeded;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Tao mot cong viec moi, chuyen thang trang thai OPEN ngay lap tuc - quyet dinh da chot
     * voi nguoi dung cho dot 1 (khong qua PENDING_REVIEW/duyet AI hay Admin). Day la hanh vi
     * TAM, se doi khi module AI (phan loai/gan co SUSPICIOUS) hoac luong Admin duyet cong
     * viec duoc hien thuc - luc do can them method rieng (vd submitForReview()) thay vi sua
     * lai constructor nay, giu factory method nay dung nghia "tao va mo cong khai ngay".
     */
    public static Task createOpen(UUID id, UUID posterId, UUID categoryId, String title, String description,
            String addressText, BigDecimal lat, BigDecimal lng, LocationType locationType, String arrivalNotes,
            SuppliesStatus suppliesStatus, String suppliesNote, Long budgetAmount, Instant scheduledAt,
            int estimatedWorkersNeeded, Instant now) {
        return new Task(id, posterId, categoryId, title, description, addressText, lat, lng, locationType,
                arrivalNotes, suppliesStatus, suppliesNote, budgetAmount, scheduledAt, estimatedWorkersNeeded,
                TaskStatus.OPEN, now);
    }

    /**
     * Poster xac nhan mot Tasker cho cong viec nay (UC11, gioi han doi trang thai - xem
     * docs/TASK-MODULE-SPLIT.md) - chuyen OPEN sang ASSIGNED. Dieu kien task dang OPEN kiem
     * tra o TaskApplicationService, khong validate lai trong entity (cung convention voi
     * KycVerification.approve()/reject()).
     */
    public void assignTo(Instant now) {
        this.status = TaskStatus.ASSIGNED;
        this.updatedAt = now;
    }

    /**
     * Ghi lai ket qua AI phan loai noi dung luc dang viec (chuc nang kiem duyet luc submit,
     * xem .claude/rules/15-ai-module.md) va gan co hau kiem cho Admin neu can. Khong con so
     * sanh voi danh muc AI de xuat - Poster tu chon danh muc hoan toan tu do, AI chi con
     * nhiem vu phat hien OTHER (ngoai 5 nhom dich vu) va SUSPICIOUS (vi pham). KHONG doi
     * status - task da OPEN tu createOpen() va luon giu OPEN du ket qua AI la gi, day la
     * quyet dinh hau kiem hoan toan da chot (khong tien kiem, khong chan luong dang viec vi
     * AI loi hay AI nghi ngo). Goi ngay sau createOpen(), truoc khi luu - tach rieng khoi
     * constructor dung theo huong dan da ghi san o Javadoc createOpen().
     */
    public void applyAiClassification(boolean needsAdminReview, TaskAiFlagReason aiFlagReason) {
        this.needsAdminReview = needsAdminReview;
        this.aiFlagReason = aiFlagReason;
    }

    /**
     * Admin xac nhan cong viec dang hau kiem la KHONG vi pham that (AI bao dong nham) - chi go
     * co needs_admin_review, KHONG doi status hay aiFlagReason (giu lai lam lich su tai sao
     * tung bi gan co, phuc vu doi chieu sau nay neu can).
     */
    public void resolveReview(UUID adminId, Instant now) {
        this.needsAdminReview = false;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    /**
     * Admin tu choi mot cong viec dang hau kiem, bat buoc kem ly do - chuyen sang REJECTED
     * (nhanh thoat da co san trong state machine, xem .claude/rules/01-domain-glossary.md).
     * Dieu kien status hien tai phai la OPEN kiem tra o TaskService, khong validate lai trong
     * entity (cung convention voi KycVerification.reject()).
     */
    public void rejectByAdmin(UUID adminId, String rejectionReason, Instant now) {
        this.status = TaskStatus.REJECTED;
        this.needsAdminReview = false;
        this.rejectionReason = rejectionReason;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    /** Id noi bo cua cong viec. */
    public UUID getId() {
        return id;
    }

    /** Id tai khoan Task Poster da dang cong viec nay. */
    public UUID getPosterId() {
        return posterId;
    }

    /** Id nhom dich vu cong viec nay thuoc ve. */
    public UUID getCategoryId() {
        return categoryId;
    }

    /** Tieu de cong viec, bat buoc phai co gia tri. */
    public String getTitle() {
        return title;
    }

    /** Mo ta chi tiet cong viec, bat buoc phai co gia tri. */
    public String getDescription() {
        return description;
    }

    /** Dia chi noi CAN THUC HIEN cong viec - doc lap voi dia chi trong ho so Poster. */
    public String getAddressText() {
        return addressText;
    }

    /** Vi do noi can thuc hien cong viec. */
    public BigDecimal getLat() {
        return lat;
    }

    /** Kinh do noi can thuc hien cong viec. */
    public BigDecimal getLng() {
        return lng;
    }

    /** Loai dia diem noi can thuc hien cong viec, null neu khong khai bao. */
    public LocationType getLocationType() {
        return locationType;
    }

    /** Luu y cho Tasker khi toi noi lam viec, null neu khong khai bao. */
    public String getArrivalNotes() {
        return arrivalNotes;
    }

    /** Tinh trang vat tu Poster da chuan bi cho cong viec nay, bat buoc phai co gia tri. */
    public SuppliesStatus getSuppliesStatus() {
        return suppliesStatus;
    }

    /** Mo ta them ve vat tu, null neu khong khai bao - tuy chon du suppliesStatus la gia tri nao. */
    public String getSuppliesNote() {
        return suppliesNote;
    }

    /** Ngan sach du kien (don vi dong), null nghia la "thoa thuan" - tuy chon, khong bat buoc. */
    public Long getBudgetAmount() {
        return budgetAmount;
    }

    /** Thoi gian mong muon thuc hien, null nghia la chua xac dinh - tuy chon, khong bat buoc. */
    public Instant getScheduledAt() {
        return scheduledAt;
    }

    /** So Tasker uoc tinh can cho cong viec nay - chi de hien thi, xem Javadoc field. */
    public int getEstimatedWorkersNeeded() {
        return estimatedWorkersNeeded;
    }

    /** Trang thai hien tai trong vong doi cong viec. */
    public TaskStatus getStatus() {
        return status;
    }

    /** Co dang cho Admin hau kiem hay khong - CHI Admin thay, khong hien thi cho Poster. */
    public boolean isNeedsAdminReview() {
        return needsAdminReview;
    }

    /** Ly do gan co hau kiem, null neu needsAdminReview = false. */
    public TaskAiFlagReason getAiFlagReason() {
        return aiFlagReason;
    }

    /** Id tai khoan Admin da xu ly (xac nhan hoac tu choi) hau kiem gan nhat, null neu chua tung xu ly. */
    public UUID getReviewedByAdminId() {
        return reviewedByAdminId;
    }

    /** Thoi diem Admin xu ly hau kiem gan nhat, null neu chua tung xu ly. */
    public Instant getReviewedAt() {
        return reviewedAt;
    }

    /** Ly do Admin tu choi, null neu chua bi tu choi lan nao. */
    public String getRejectionReason() {
        return rejectionReason;
    }

    /** Thoi diem dang cong viec, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem cap nhat gan nhat. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
