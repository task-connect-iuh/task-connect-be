package vn.taskconnect.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 1 lan Poster bam Luu o form sua cong viec (UC07), CHI GHI THEM - khong UPDATE/DELETE dong da
 * co. Cac truong cu the da doi nam o {@link TaskEditChange}. Xem
 * V45__create_task_edit_event_tables.sql.
 */
@Entity
@Table(name = "task_edit_events")
public class TaskEditEvent {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "task_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID taskId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "edited_by_account_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID editedByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TaskEditEvent() {
        // JPA
    }

    /** Tao 1 su kien sua viec moi - luon do chinh Poster cua task thuc hien (UC07). */
    public static TaskEditEvent of(UUID id, UUID taskId, UUID editedByAccountId, Instant now) {
        TaskEditEvent event = new TaskEditEvent();
        event.id = id;
        event.taskId = taskId;
        event.editedByAccountId = editedByAccountId;
        event.createdAt = now;
        return event;
    }

    /** Id noi bo cua su kien sua viec nay. */
    public UUID getId() {
        return id;
    }

    /** Id cong viec da bi sua. */
    public UUID getTaskId() {
        return taskId;
    }

    /** Id tai khoan Poster da thuc hien lan sua nay. */
    public UUID getEditedByAccountId() {
        return editedByAccountId;
    }

    /** Thoi diem sua, khong doi sau do. */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
