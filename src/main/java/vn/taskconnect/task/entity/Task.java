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
import vn.taskconnect.task.api.TaskStatus;

/**
 * Mot cong viec do Task Poster dang. Xem V18__create_task_tables.sql. Dot 1 (dang viec toi
 * gian) chi ho tro tao va doc - khong co method chuyen trang thai nao khac ngoai createOpen()
 * (sua/huy UC07, giao viec UC11 se them method rieng, theo dung mau
 * KycVerification.approve()/reject() khi lam dot do). addressText/lat/lng la dia diem CAN
 * THUC HIEN cong viec, hoan toan doc lap voi addressText/locationLat/locationLng cua
 * user_profiles - khong tham chieu qua lai.
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {
        // JPA
    }

    private Task(UUID id, UUID posterId, UUID categoryId, String title, String description, String addressText,
            BigDecimal lat, BigDecimal lng, Long budgetAmount, Instant scheduledAt, int estimatedWorkersNeeded,
            TaskStatus status, Instant now) {
        this.id = id;
        this.posterId = posterId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.addressText = addressText;
        this.lat = lat;
        this.lng = lng;
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
            String addressText, BigDecimal lat, BigDecimal lng, Long budgetAmount, Instant scheduledAt,
            int estimatedWorkersNeeded, Instant now) {
        return new Task(id, posterId, categoryId, title, description, addressText, lat, lng, budgetAmount,
                scheduledAt, estimatedWorkersNeeded, TaskStatus.OPEN, now);
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

    /** Thoi diem dang cong viec, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem cap nhat gan nhat. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
