package vn.taskconnect.task.entity;

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
import vn.taskconnect.task.api.ExtraCostBatchStatus;

/**
 * Mot lan Tasker dang chi phi phat sinh (task_extra_cost_batches, co the gom nhieu khoan - xem
 * TaskExtraCostItem), xem V50. Chi dung khi booking cua application nay chon FULL_ESCROW (quyet
 * dinh nguoi dung 2026-10-02, kiem tra o TaskExtraCostService.submit, khong validate lai trong
 * entity - cung convention voi Task.java/TaskApplication.java).
 */
@Entity
@Table(name = "task_extra_cost_batches")
public class TaskExtraCostBatch {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "batch_no", nullable = false, updatable = false)
    private int batchNo;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "application_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExtraCostBatchStatus status;

    @JdbcTypeCode(SqlTypes.BIGINT)
    @Column(name = "total_amount", nullable = false, updatable = false)
    private long totalAmount;

    @Column(name = "note", length = 500, updatable = false)
    private String note;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "submitted_by_account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID submittedByAccountId;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "reviewed_by_account_id", columnDefinition = "BINARY(16)")
    private UUID reviewedByAccountId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskExtraCostBatch() {
        // JPA
    }

    /** Tasker dang 1 batch chi phi phat sinh moi - luon bat dau PENDING. batchNo tinh san boi TaskExtraCostService (dem theo application). */
    public static TaskExtraCostBatch submit(UUID id, int batchNo, UUID applicationId, long totalAmount, String note,
            UUID submittedByAccountId, Instant now) {
        TaskExtraCostBatch batch = new TaskExtraCostBatch();
        batch.id = id;
        batch.batchNo = batchNo;
        batch.applicationId = applicationId;
        batch.status = ExtraCostBatchStatus.PENDING;
        batch.totalAmount = totalAmount;
        batch.note = note;
        batch.submittedByAccountId = submittedByAccountId;
        batch.submittedAt = now;
        batch.createdAt = now;
        batch.updatedAt = now;
        return batch;
    }

    /** Poster dong y batch nay - cong totalAmount vao requiredTotal (xem TaskExtraCostService.buildSummary). Chi goi khi dang PENDING. */
    public void approve(UUID reviewerAccountId, Instant now) {
        this.status = ExtraCostBatchStatus.APPROVED;
        this.reviewedByAccountId = reviewerAccountId;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    /** Poster tu choi batch nay - khong tinh vao requiredTotal. Chi goi khi dang PENDING. */
    public void reject(UUID reviewerAccountId, Instant now) {
        this.status = ExtraCostBatchStatus.REJECTED;
        this.reviewedByAccountId = reviewerAccountId;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    /** Tasker tu thu hoi batch do chinh minh dang - chi goi khi dang PENDING. */
    public void withdraw(Instant now) {
        this.status = ExtraCostBatchStatus.WITHDRAWN;
        this.withdrawnAt = now;
        this.updatedAt = now;
    }

    /** Id noi bo cua batch nay. */
    public UUID getId() {
        return id;
    }

    /** So thu tu hien thi, tinh rieng theo tung application (vd "Phat sinh #2"). */
    public int getBatchNo() {
        return batchNo;
    }

    /** Id don ung tuyen (da duoc chon o UC11) ma batch nay thuoc ve. */
    public UUID getApplicationId() {
        return applicationId;
    }

    /** Trang thai hien tai cua batch. */
    public ExtraCostBatchStatus getStatus() {
        return status;
    }

    /** Tong tien cua batch - cache = SUM(items.amount) luc dang, khong tinh lai sau do. */
    public long getTotalAmount() {
        return totalAmount;
    }

    /** Ghi chu chung cho ca batch, null neu khong nhap. */
    public String getNote() {
        return note;
    }

    /** Id tai khoan Tasker da dang batch nay. */
    public UUID getSubmittedByAccountId() {
        return submittedByAccountId;
    }

    /** Thoi diem dang, khong doi sau do. */
    public Instant getSubmittedAt() {
        return submittedAt;
    }

    /** Id tai khoan Poster da duyet/tu choi, null neu chua xu ly. */
    public UUID getReviewedByAccountId() {
        return reviewedByAccountId;
    }

    /** Thoi diem Poster duyet/tu choi, null neu chua xu ly. */
    public Instant getReviewedAt() {
        return reviewedAt;
    }

    /** Thoi diem Tasker thu hoi, null neu chua tung thu hoi. */
    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    /** Thoi diem tao, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem cap nhat gan nhat. */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
