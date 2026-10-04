package vn.taskconnect.task.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.taskconnect.task.api.TaskEditableField;

/**
 * Chi tiet "truoc -&gt; sau" cua 1 lan Poster sua cong viec (UC07), doc qua
 * {@link vn.taskconnect.task.api.TaskFacade#findTaskEdit(UUID)}. Dung boi Chat khi nguoi dung
 * bam nut "Xem chi tiet thay doi" tren SYSTEM message - xem
 * V45__create_task_edit_event_tables.sql. Chi mang gia tri THO (so dong, ten enum, ISO-8601);
 * dinh dang/nhan tieng Viet do FE tu xu ly.
 */
public record TaskEditSummary(UUID id, UUID taskId, Instant editedAt, List<Change> changes) {

    public record Change(TaskEditableField field, String oldValue, String newValue) {
    }
}
