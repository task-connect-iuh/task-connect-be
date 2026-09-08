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
import vn.taskconnect.task.api.TaskApplicationStatus;

/**
 * Mot don ung tuyen cua Tasker vao mot cong viec (UC10). Xem V19__create_task_application_table.sql.
 * Chuyen trang thai khong tu validate ben trong entity (giong Task.java/KycVerification.java) -
 * dieu kien hop le (task dang OPEN, don dang PENDING...) kiem tra o TaskApplicationService.
 */
@Entity
@Table(name = "task_applications")
public class TaskApplication {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "task_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID taskId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "tasker_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID taskerId;

    // Da bo khoi form ung tuyen (yeu cau nguoi dung) - giu cot nullable de tuong thich nguoc,
    // khong bat buoc nua.
    @Column(name = "proposed_arrival_text", length = 200, updatable = false)
    private String proposedArrivalText;

    @Column(name = "message", columnDefinition = "TEXT", updatable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskApplicationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected TaskApplication() {
        // JPA
    }

    /** Tao mot don ung tuyen moi, luon bat dau o trang thai PENDING. */
    public static TaskApplication submit(UUID id, UUID taskId, UUID taskerId, String proposedArrivalText,
            String message, Instant now) {
        TaskApplication application = new TaskApplication();
        application.id = id;
        application.taskId = taskId;
        application.taskerId = taskerId;
        application.proposedArrivalText = proposedArrivalText;
        application.message = message;
        application.status = TaskApplicationStatus.PENDING;
        application.createdAt = now;
        return application;
    }

    /** Poster xac nhan don nay - chuyen ACCEPTED, ghi nhan thoi diem phan hoi. */
    public void accept(Instant now) {
        this.status = TaskApplicationStatus.ACCEPTED;
        this.respondedAt = now;
    }

    /** Poster tu choi don nay - chuyen REJECTED, ghi nhan thoi diem phan hoi. */
    public void reject(Instant now) {
        this.status = TaskApplicationStatus.REJECTED;
        this.respondedAt = now;
    }

    /**
     * Cac don PENDING con lai cua cung 1 task tu dong chuyen sang khi Poster da xac nhan mot
     * ung vien khac (OQ-08: he thong chi ho tro 1 Tasker/task).
     */
    public void markNeedsReconfirm(Instant now) {
        this.status = TaskApplicationStatus.NEEDS_RECONFIRM;
        this.respondedAt = now;
    }

    /** Id noi bo cua don ung tuyen nay. */
    public UUID getId() {
        return id;
    }

    /** Id cong viec duoc ung tuyen. */
    public UUID getTaskId() {
        return taskId;
    }

    /** Id tai khoan Tasker da gui don. */
    public UUID getTaskerId() {
        return taskerId;
    }

    /** Thoi gian Tasker de xuat toi lam, van ban tu do nguoi dung tu nhap. */
    public String getProposedArrivalText() {
        return proposedArrivalText;
    }

    /** Loi nhan ngan Tasker gui kem don ung tuyen, null neu khong nhap. */
    public String getMessage() {
        return message;
    }

    /** Trang thai hien tai cua don ung tuyen. */
    public TaskApplicationStatus getStatus() {
        return status;
    }

    /** Thoi diem gui don, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem Poster phan hoi (xac nhan/tu choi) hoac tu dong chuyen NEEDS_RECONFIRM, null neu con PENDING. */
    public Instant getRespondedAt() {
        return respondedAt;
    }
}
