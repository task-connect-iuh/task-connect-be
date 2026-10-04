package vn.taskconnect.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.taskconnect.task.api.TaskEditableField;

/**
 * 1 truong da thay doi gia tri trong 1 lan sua viec (UC07) - CHI ghi nhung truong THAT SU doi,
 * khong ghi truong Poster gui len nhung gia tri khong khac gi truoc do. oldValue/newValue la
 * gia tri THO (so dong, ten enum, chuoi ISO-8601) - FE tu dinh dang/dich nhan hien thi. Xem
 * V45__create_task_edit_event_tables.sql.
 */
@Entity
@Table(name = "task_edit_event_changes")
public class TaskEditChange {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "event_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "field", nullable = false, length = 20, updatable = false)
    private TaskEditableField field;

    @Column(name = "old_value", columnDefinition = "TEXT", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT", updatable = false)
    private String newValue;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "sort_order", nullable = false, updatable = false)
    private int sortOrder;

    protected TaskEditChange() {
        // JPA
    }

    /** Ghi nhan 1 truong da doi gia tri trong 1 su kien sua viec - thu tu hien thi theo sortOrder. */
    public static TaskEditChange of(UUID id, UUID eventId, TaskEditableField field, String oldValue,
            String newValue, int sortOrder) {
        TaskEditChange change = new TaskEditChange();
        change.id = id;
        change.eventId = eventId;
        change.field = field;
        change.oldValue = oldValue;
        change.newValue = newValue;
        change.sortOrder = sortOrder;
        return change;
    }

    /** Id noi bo cua dong thay doi nay. */
    public UUID getId() {
        return id;
    }

    /** Id su kien sua viec ma dong nay thuoc ve. */
    public UUID getEventId() {
        return eventId;
    }

    /** Truong da doi gia tri. */
    public TaskEditableField getField() {
        return field;
    }

    /** Gia tri THO truoc khi sua, null neu truoc do chua co gia tri. */
    public String getOldValue() {
        return oldValue;
    }

    /** Gia tri THO sau khi sua, null neu Poster xoa het gia tri. */
    public String getNewValue() {
        return newValue;
    }

    /** Thu tu hien thi trong danh sach thay doi (khop thu tu khai bao TaskEditableField). */
    public int getSortOrder() {
        return sortOrder;
    }
}
