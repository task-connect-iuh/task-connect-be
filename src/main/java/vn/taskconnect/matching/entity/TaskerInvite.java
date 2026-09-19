package vn.taskconnect.matching.entity;

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
import vn.taskconnect.matching.api.TaskerInviteStatus;

/**
 * Mot loi moi Poster gui truc tiep cho mot Tasker cho mot Task cu the (luong "Poster moi
 * Tasker", song song voi TaskApplication/UC10 - xem V26__create_matching_tables.sql).
 * Mirror TaskApplication.java: chuyen trang thai khong tu validate ben trong entity, dieu
 * kien hop le (task dang OPEN, chua moi trung Tasker nay...) kiem tra o TaskerInviteService.
 */
@Entity
@Table(name = "matching_tasker_invites")
public class TaskerInvite {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskerInviteStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected TaskerInvite() {
        // JPA
    }

    /** Tao mot loi moi moi, luon bat dau o trang thai PENDING. */
    public static TaskerInvite create(UUID id, UUID taskId, UUID taskerId, Instant now) {
        TaskerInvite invite = new TaskerInvite();
        invite.id = id;
        invite.taskId = taskId;
        invite.taskerId = taskerId;
        invite.status = TaskerInviteStatus.PENDING;
        invite.createdAt = now;
        return invite;
    }

    /** Tasker chap nhan loi moi nay - chuyen ACCEPTED, ghi nhan thoi diem phan hoi. */
    public void accept(Instant now) {
        this.status = TaskerInviteStatus.ACCEPTED;
        this.respondedAt = now;
    }

    /** Tasker tu choi loi moi nay - chuyen DECLINED, ghi nhan thoi diem phan hoi. */
    public void decline(Instant now) {
        this.status = TaskerInviteStatus.DECLINED;
        this.respondedAt = now;
    }

    /** Danh dau loi moi het han vi Tasker khong phan hoi (job don dep dinh ky, chua hien thuc dot nay). */
    public void expire(Instant now) {
        this.status = TaskerInviteStatus.EXPIRED;
        this.respondedAt = now;
    }

    /** Id noi bo cua loi moi nay. */
    public UUID getId() {
        return id;
    }

    /** Id cong viec duoc moi. */
    public UUID getTaskId() {
        return taskId;
    }

    /** Id tai khoan Tasker duoc moi. */
    public UUID getTaskerId() {
        return taskerId;
    }

    /** Trang thai hien tai cua loi moi. */
    public TaskerInviteStatus getStatus() {
        return status;
    }

    /** Thoi diem tao loi moi, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Thoi diem Tasker phan hoi (chap nhan/tu choi) hoac tu dong EXPIRED, null neu con PENDING. */
    public Instant getRespondedAt() {
        return respondedAt;
    }
}
